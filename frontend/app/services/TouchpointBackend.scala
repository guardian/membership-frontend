package services

import akka.actor.ActorSystem
import com.gu.config.MembershipRatePlanIds
import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.services.PaymentService
import com.gu.memsub.subsv2
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.services.FetchCatalog
import com.gu.memsub.subsv2.services.SubscriptionService.CatalogMap
import com.gu.okhttp.RequestRunners
import com.gu.salesforce._
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.touchpoint.TouchpointBackendConfig
import com.gu.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.rest.{SimpleClient, ZuoraRestService}
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{soap, ZuoraSoapService => ZuoraSoapServiceImpl}
import com.netaporter.uri.Uri
import com.gu.monitoring.SafeLogger
import configuration.Config
import model.FeatureChoice
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import utils.TestUsers.{TestUserCredentialType, isTestUser}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scalaz.std.scalaFuture._

object TouchpointBackend {
  implicit class TouchpointBackendConfigLike(tpbc: TouchpointBackendConfig) {
    def zuoraEnvName: String = tpbc.zuoraSoap.envName
    def zuoraRestUrl(config: com.typesafe.config.Config): String =
      config.getString(s"touchpoint.backend.environments.$zuoraEnvName.zuora.api.restUrl")
  }

  def apply(backendType: BackendType)(implicit system: ActorSystem, executionContext: ExecutionContext, wsClient: WSClient): TouchpointBackend = {
    val backendConfig = TouchpointBackendConfig.byType(backendType, Config.config)
    TouchpointBackend(backendConfig, backendType)
  }

  def apply(config: TouchpointBackendConfig, backendType: BackendType)(implicit system: ActorSystem, executionContext: ExecutionContext, wsClient: WSClient): TouchpointBackend = {
    val stripeUKMembershipService = new StripeService(
      apiConfig = config.stripeUKMembership,
      client = RequestRunners.futureRunner
    )
    val stripeAUMembershipService = new StripeService(
      apiConfig = config.stripeAUMembership,
      client = RequestRunners.futureRunner
    )
    val payPalService = new PayPalService(config.payPal, executionContext)
    val restBackendConfig = config.zuoraRest.copy(url = Uri.parse(config.zuoraRestUrl(Config.config)))
    implicit val simpleRestClient = new SimpleClient[Future](restBackendConfig, RequestRunners.futureRunner)
    val memRatePlanIds = Config.membershipRatePlanIds(restBackendConfig.envName)
    val paperRatePlanIds = Config.subsProductIds(restBackendConfig.envName)
    val digipackRatePlanIds = Config.digipackRatePlanIds(restBackendConfig.envName)
    val runner = RequestRunners.futureRunner
    // extendedRunner sets the configurable read timeout, which is used for the createSubscription call.
    val extendedRunner = RequestRunners.configurableFutureRunner(20.seconds)

    val zuoraSoapClient = new ClientWithFeatureSupplier(FeatureChoice.codes, config.zuoraSoap, runner, extendedRunner)
    val discounter = new Discounter(Config.discountRatePlanIds(config.zuoraEnvName))
    val zuoraSoapService = new ZuoraSoapServiceImpl(zuoraSoapClient)
    val zuoraRestService = new ZuoraRestService[Future]

    val pids = Config.productIds(restBackendConfig.envName)

    val catalogRestClient: SimpleClient[Future] = new SimpleClient[Future](restBackendConfig, RequestRunners.configurableFutureRunner(60.seconds))
    val newCatalogService = new subsv2.services.CatalogService[Future](pids, FetchCatalog.fromZuoraApi(catalogRestClient), Await.result(_, 60.seconds), restBackendConfig.envName)
    val futureCatalog: Future[CatalogMap] = newCatalogService.catalog.map(_.fold[CatalogMap](error => {println(s"error: ${error.list.toList.mkString}"); Map()}, _.map))
    val newSubsService = new subsv2.services.SubscriptionService[Future](pids, futureCatalog, simpleRestClient, zuoraSoapService.getAccountIds)

    val paymentService = new PaymentService(zuoraSoapService, newCatalogService.unsafeCatalog.productMap)
    val salesforceService = new SalesforceService(config.salesforce)
    val identityService = IdentityService(new IdentityApi(wsClient, executionContext))
    val memberService = new MemberService(
      identityService = identityService,
      salesforceService = salesforceService,
      zuoraService = zuoraSoapService,
      zuoraRestService = zuoraRestService,
      ukStripeService = stripeUKMembershipService,
      auStripeService = stripeAUMembershipService,
      payPalService = payPalService,
      subscriptionService = newSubsService,
      catalogService = newCatalogService,
      paymentService = paymentService,
      discounter = discounter,
      discountIds = Config.discountRatePlanIds(config.zuoraEnvName),
      invoiceIdsByCountry = Config.invoiceTemplateOverrides(config.zuoraEnvName),
      ec = executionContext
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
      zuoraService = zuoraSoapService,
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

class TouchpointBackends(actorSystem: ActorSystem, executionContext: ExecutionContext, wsClient: WSClient) {

  import TouchpointBackend._
  import TouchpointBackendConfig.BackendType

  implicit private val as = actorSystem
  implicit private val ec = executionContext
  implicit private val ws = wsClient

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
    SafeLogger.info(s"TouchpointBackend.TestUser is lazily initialised to ensure bad UAT settings can not block deployment to PROD. Initalisation starting...")
    val amountOfPlans = TestUser.catalog.allMembership.size
    SafeLogger.info(s"TouchpointBackend.TestUser initalisation complete: $amountOfPlans membership plans in UAT")
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
) {

  lazy val catalog: Catalog = catalogService.unsafeCatalog
}
