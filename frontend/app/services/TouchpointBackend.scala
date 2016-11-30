package services

import com.gu.config.MembershipRatePlanIds
import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.promo.{DynamoPromoCollection, PromotionCollection}
import com.gu.memsub.services.{PaymentService, PromoService, api => memsubapi}
import com.gu.memsub.subsv2
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.services.SubscriptionService.CatalogMap
import com.gu.monitoring.{ServiceMetrics, StatusMetrics}
import com.gu.okhttp.RequestRunners
import com.gu.salesforce._
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.touchpoint.TouchpointBackendConfig
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.rest.SimpleClient
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{rest, soap, ZuoraService => ZuoraServiceImpl}
import com.netaporter.uri.Uri
import configuration.Config
import configuration.Config.Implicits.akkaSystem
import model.FeatureChoice
import monitoring.TouchpointBackendMetrics
import org.joda.time.LocalDate
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import tracking._
import utils.TestUsers.isTestUser
import org.joda.time.LocalDate
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.std.scalaFuture._

object TouchpointBackend {

  import TouchpointBackendConfig.BackendType

  implicit class TouchpointBackendConfigLike(tpbc: TouchpointBackendConfig) {
    def zuoraEnvName: String = tpbc.zuoraSoap.envName
    def zuoraMetrics(component: String): ServiceMetrics = new ServiceMetrics(zuoraEnvName, "membership", component)
    def zuoraRestUrl(config: com.typesafe.config.Config): String =
      config.getString(s"touchpoint.backend.environments.$zuoraEnvName.zuora.api.restUrl")
  }

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend = {
    val touchpointBackendConfig = TouchpointBackendConfig.byType(backendType, Config.config)
    TouchpointBackend(touchpointBackendConfig, backendType)
  }

  def apply(backend: TouchpointBackendConfig, backendType: BackendType): TouchpointBackend = {
    val stripeService = new StripeService(backend.stripe, RequestRunners.loggingRunner(new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = backend.stripe.envName
      val service = "Stripe"
    }))
    val giraffeStripeService = new StripeService(backend.giraffe, RequestRunners.loggingRunner(new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = backend.stripe.envName
      val service = "Stripe Giraffe"
    }))

    val restBackendConfig = backend.zuoraRest.copy(url = Uri.parse(backend.zuoraRestUrl(Config.config)))

    val memRatePlanIds = Config.membershipRatePlanIds(restBackendConfig.envName)
    val paperRatePlanIds = Config.subsProductIds(restBackendConfig.envName)
    val digipackRatePlanIds = Config.digipackRatePlanIds(restBackendConfig.envName)
    val runner = RequestRunners.loggingRunner(backend.zuoraMetrics("zuora-soap-client"))
    // extendedRunner sets the configurable read timeout, which is used for the createSubscription call.
    val extendedRunner = RequestRunners.configurableLoggingRunner(20.seconds, backend.zuoraMetrics("zuora-soap-client"))
    val zuoraSoapClient = new ClientWithFeatureSupplier(FeatureChoice.codes, backend.zuoraSoap, runner, extendedRunner)

    val discounter = new Discounter(Config.discountRatePlanIds(backend.zuoraEnvName))
    val promoCollection = DynamoPromoCollection.forStage(Config.config, restBackendConfig.envName)
    val promoService = new PromoService(promoCollection, discounter)
    val zuoraService = new ZuoraServiceImpl(zuoraSoapClient)

    val pids = Config.productIds(restBackendConfig.envName)
    val client = new SimpleClient[Future](restBackendConfig, RequestRunners.futureRunner)
    val newCatalogService = new subsv2.services.CatalogService[Future](pids, client, Await.result(_, 10.seconds), restBackendConfig.envName)
    val futureCatalog: Future[CatalogMap] = newCatalogService.catalog.map(_.fold[CatalogMap](error => {println(s"error: ${error.list.mkString}"); Map()}, _.map))
    val newSubsService = new subsv2.services.SubscriptionService[Future](pids, futureCatalog, client, zuoraService.getAccountIds, () => LocalDate.now)

    val paymentService = new PaymentService(stripeService, zuoraService, newCatalogService.unsafeCatalog.productMap)
    val salesforceService = new SalesforceService(backend.salesforce)
    val identityService = IdentityService(IdentityApi)
    val memberService = new MemberService(
      identityService, salesforceService, zuoraService, stripeService, newSubsService, newCatalogService, promoService, paymentService, discounter,
        Config.discountRatePlanIds(backend.zuoraEnvName))

    TouchpointBackend(
      salesforceService = salesforceService,
      stripeService = stripeService,
      giraffeStripeService = giraffeStripeService,
      zuoraSoapClient = zuoraSoapClient,
      destinationService = new DestinationService[Future](
        EventbriteService.getBookableEvent,
        GuardianContentService.contentItemQuery,
        memberService.createEBCode
      ),
      memberService = memberService,
      subscriptionService = newSubsService,
      catalogService = newCatalogService,
      zuoraService = zuoraService,
      promoService = promoService,
      promos = promoCollection,
      membershipRatePlanIds = memRatePlanIds,
      paymentService = paymentService,
      identityService = identityService
    )
  }

  lazy val Normal = TouchpointBackend(BackendType.Default)
  lazy val TestUser = TouchpointBackend(BackendType.Testing)

  def forUser(user: IdMinimalUser): TouchpointBackend = if (isTestUser(user)) TestUser else Normal
  // Convenience method for Salesforce users. Assumes firstName matches the test user key generated by the app
  def forUser(user: Contact): TouchpointBackend = if (isTestUser(user)) TestUser else Normal
}

case class TouchpointBackend(salesforceService: api.SalesforceService,
                             stripeService: StripeService,
                             giraffeStripeService: StripeService,
                             zuoraSoapClient: soap.ClientWithFeatureSupplier,
                             destinationService: DestinationService[Future],
                             memberService: api.MemberService,
                             subscriptionService: subsv2.services.SubscriptionService[Future],
                             catalogService: subsv2.services.CatalogService[Future],
                             zuoraService: ZuoraService,
                             membershipRatePlanIds: MembershipRatePlanIds,
                             promos: PromotionCollection,
                             promoService: PromoService,
                             paymentService: PaymentService,
                             identityService: IdentityService) extends ActivityTracking {

  lazy val catalog: Catalog = catalogService.unsafeCatalog
}
