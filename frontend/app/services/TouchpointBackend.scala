package services

import akka.actor.ActorSystem
import com.gu.config.MembershipRatePlanIds
import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.services.{PaymentService, api => memsubapi}
import com.gu.memsub.subsv2
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.services.SubscriptionService.CatalogMap
import com.gu.monitoring.{ServiceMetrics, StatusMetrics}
import com.gu.okhttp.RequestRunners
import com.gu.salesforce._
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.touchpoint.TouchpointBackendConfig
import com.gu.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.rest.SimpleClient
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{ZuoraRestService, soap, ZuoraService => ZuoraServiceImpl}
import com.netaporter.uri.Uri
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.FeatureChoice
import monitoring.TouchpointBackendMetrics
import play.api.libs.ws.WS
import play.api.mvc.RequestHeader
import play.api.Play.current
import tracking._
import utils.TestUsers.{TestUserCredentialType, isTestUser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.std.scalaFuture._

object TouchpointBackend {
  implicit class TouchpointBackendConfigLike(tpbc: TouchpointBackendConfig) {
    def zuoraEnvName: String = tpbc.zuoraSoap.envName
    def zuoraMetrics(component: String): ServiceMetrics = new ServiceMetrics(zuoraEnvName, "membership", component)
    def zuoraRestUrl(config: com.typesafe.config.Config): String =
      config.getString(s"touchpoint.backend.environments.$zuoraEnvName.zuora.api.restUrl")
  }

  def apply(backendType: BackendType)(implicit system: ActorSystem): TouchpointBackend = {
    val backendConfig = TouchpointBackendConfig.byType(backendType, Config.config)
    TouchpointBackend(backendConfig, backendType)
  }

  def apply(config: TouchpointBackendConfig, backendType: BackendType)(implicit system: ActorSystem): TouchpointBackend = {
    val stripeUKMembershipService = new StripeService(
      apiConfig = config.stripeUKMembership,
      client = RequestRunners.loggingRunner(new TouchpointBackendMetrics with StatusMetrics {
        val backendEnv: String = config.stripeUKMembership.envName
        val service = "Stripe UK Membership"
      })
    )
    val stripeAUMembershipService = new StripeService(
      apiConfig = config.stripeAUMembership,
      client = RequestRunners.loggingRunner(new TouchpointBackendMetrics with StatusMetrics {
        val backendEnv: String = config.stripeAUMembership.envName
        val service = "Stripe AU Membership"
      })
    )
    val payPalService = new PayPalService(config.payPal)
    val restBackendConfig = config.zuoraRest.copy(url = Uri.parse(config.zuoraRestUrl(Config.config)))
    implicit val simpleRestClient = new SimpleClient[Future](restBackendConfig, RequestRunners.futureRunner)
    val memRatePlanIds = Config.membershipRatePlanIds(restBackendConfig.envName)
    val paperRatePlanIds = Config.subsProductIds(restBackendConfig.envName)
    val digipackRatePlanIds = Config.digipackRatePlanIds(restBackendConfig.envName)
    val runner = RequestRunners.loggingRunner(config.zuoraMetrics("zuora-soap-client"))
    // extendedRunner sets the configurable read timeout, which is used for the createSubscription call.
    val extendedRunner = RequestRunners.configurableLoggingRunner(20.seconds, config.zuoraMetrics("zuora-soap-client"))

    val zuoraSoapClient = new ClientWithFeatureSupplier(FeatureChoice.codes, config.zuoraSoap, runner, extendedRunner)
    val discounter = new Discounter(Config.discountRatePlanIds(config.zuoraEnvName))
    val zuoraService = new ZuoraServiceImpl(zuoraSoapClient)
    val zuoraRestService = new ZuoraRestService[Future]()

    val pids = Config.productIds(restBackendConfig.envName)

    val newCatalogService = new subsv2.services.CatalogService[Future](pids, simpleRestClient, Await.result(_, 10.seconds), restBackendConfig.envName)
    val futureCatalog: Future[CatalogMap] = newCatalogService.catalog.map(_.fold[CatalogMap](error => {println(s"error: ${error.list.mkString}"); Map()}, _.map))
    val newSubsService = new subsv2.services.SubscriptionService[Future](pids, futureCatalog, simpleRestClient, zuoraService.getAccountIds)

    val paymentService = new PaymentService(zuoraService, newCatalogService.unsafeCatalog.productMap)
    val salesforceService = new SalesforceService(config.salesforce)
    val identityService = IdentityService(new IdentityApi(WS.client))
    val memberService = new MemberService(
      identityService = identityService,
      salesforceService = salesforceService,
      zuoraService = zuoraService,
      zuoraRestService = zuoraRestService,
      ukStripeService = stripeUKMembershipService,
      auStripeService = stripeAUMembershipService,
      payPalService = payPalService,
      subscriptionService = newSubsService,
      catalogService = newCatalogService,
      paymentService = paymentService,
      discounter = discounter,
      discountIds = Config.discountRatePlanIds(config.zuoraEnvName),
      invoiceIdsByCountry = Config.invoiceTemplateOverrides(config.zuoraEnvName)
    )

    TouchpointBackend(
      config.environmentName,
      salesforceService = salesforceService,
      payPalService = payPalService,
      stripeUKMembershipService = stripeUKMembershipService,
      stripeAUMembershipService = stripeAUMembershipService,
      zuoraSoapClient = zuoraSoapClient,
      memberService = memberService,
      subscriptionService = newSubsService,
      catalogService = newCatalogService,
      zuoraService = zuoraService,
      zuoraRestService = zuoraRestService,
      membershipRatePlanIds = memRatePlanIds,
      paymentService = paymentService,
      identityService = identityService,
      simpleRestClient = simpleRestClient
    )
  }

  case class Resolution(
    backend: TouchpointBackend,
    typ: BackendType,
    validTestUserCredentialOpt: Option[TestUserCredentialType[_]]
  )
}

class TouchpointBackendProvider(implicit val system: ActorSystem) extends LazyLogging {

  import TouchpointBackend._
  import TouchpointBackendConfig.BackendType

  // TestUser (especially) has to be lazy as otherwise the app can't come up without the test catalog being valid.
  lazy val Normal = TouchpointBackend(BackendType.Default)
  lazy val TestUser = TouchpointBackend(BackendType.Testing)

  def backendFor(backendType: BackendType): TouchpointBackend = backendType match {
    case BackendType.Testing => TestUser
    case BackendType.Default => Normal
  }

  /**
    * Alternate credentials are used *only* when the user is not signed in - if you're logged in as
    * a 'normal' non-test user, it doesn't make any difference what pre-signin-test-cookie you have.
    */
  def forRequest[C](permittedAltCredentialType: TestUserCredentialType[C], altCredentialSource: C)(
    implicit request: RequestHeader): Resolution = {
    val validTestUserCredentialOpt = isTestUser(permittedAltCredentialType, altCredentialSource)
    val backendType = if (validTestUserCredentialOpt.isDefined) BackendType.Testing else BackendType.Default
    Resolution(backendFor(backendType), backendType, validTestUserCredentialOpt)
  }

  Future {
    logger.info(s"TouchpointBackend.TestUser is lazily initialised to ensure bad UAT settings can not block deployment to PROD. Initalisation starting...")
    val amountOfPlans = TestUser.catalog.allMembership.size
    logger.info(s"TouchpointBackend.TestUser initalisation complete: $amountOfPlans membership plans in UAT")
  }

  def forUser(user: IdMinimalUser): TouchpointBackend = if (isTestUser(user)) TestUser else Normal
  // Convenience method for Salesforce users. Assumes firstName matches the test user key generated by the app
  def forUser(user: Contact): TouchpointBackend = if (isTestUser(user)) TestUser else Normal
}

case class TouchpointBackend(
  environmentName: String,
  salesforceService: api.SalesforceService,
  payPalService: PayPalService,
  stripeUKMembershipService: StripeService,
  stripeAUMembershipService: StripeService,
  zuoraSoapClient: soap.ClientWithFeatureSupplier,
  memberService: api.MemberService,
  subscriptionService: subsv2.services.SubscriptionService[Future],
  catalogService: subsv2.services.CatalogService[Future],
  zuoraService: ZuoraService,
  zuoraRestService: ZuoraRestService[Future],
  membershipRatePlanIds: MembershipRatePlanIds,
  paymentService: PaymentService,
  identityService: IdentityService,
  simpleRestClient: SimpleClient[Future]
) extends ActivityTracking {

  lazy val catalog: Catalog = catalogService.unsafeCatalog
}
