package services

import akka.actor.ActorSystem
import com.gu.memsub.subsv2
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.services.FetchCatalog
import com.gu.memsub.subsv2.services.SubscriptionService.CatalogMap
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.okhttp.RequestRunners
import com.gu.salesforce._
import com.gu.touchpoint.TouchpointBackendConfig
import com.gu.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.zuora.rest.SimpleClient
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{soap, ZuoraSoapService => ZuoraSoapServiceImpl}
import configuration.Config
import io.lemonlabs.uri.Uri
import model.{FeatureChoice, IdMinimalUser}
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import scalaz.std.scalaFuture._
import utils.TestUsers
import utils.TestUsers.{TestUserCredentialType, isTestUser}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

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
    val restBackendConfig = config.zuoraRest.copy(url = Uri.parse(config.zuoraRestUrl(Config.config)))
    implicit val simpleRestClient = new SimpleClient[Future](restBackendConfig, RequestRunners.futureRunner)
    val runner = RequestRunners.futureRunner
    // extendedRunner sets the configurable read timeout, which is used for the createSubscription call.
    val extendedRunner = RequestRunners.configurableFutureRunner(20.seconds)

    val zuoraSoapClient = new ClientWithFeatureSupplier(FeatureChoice.codes, config.zuoraSoap, runner, extendedRunner)
    val zuoraSoapService = new ZuoraSoapServiceImpl(zuoraSoapClient)

    val pids = Config.productIds(restBackendConfig.envName)

    val catalogRestClient: SimpleClient[Future] = new SimpleClient[Future](restBackendConfig, RequestRunners.configurableFutureRunner(60.seconds))
    val newCatalogService = new subsv2.services.CatalogService[Future](pids, FetchCatalog.fromZuoraApi(catalogRestClient), Await.result(_, 60.seconds), restBackendConfig.envName)

    val futureCatalog: Future[CatalogMap] = newCatalogService.catalog
      .map(_.fold[CatalogMap](error => {println(s"error: ${error.list.toList.mkString}"); Map()}, _.map))
      .recover {
        case error =>
          SafeLogger.error(scrub"Failed to load catalog from Zuora due to: $error")
          throw error
      }

    val newSubsService = new subsv2.services.SubscriptionService[Future](pids, futureCatalog, simpleRestClient, zuoraSoapService.getAccountIds)

    val salesforceService = new SalesforceService(config.salesforce)
    val identityService = IdentityService(new IdentityApi(wsClient, executionContext))
    val memberService = new MemberService()(executionContext)

    TouchpointBackend(
      config.environmentName,
      salesforceService = salesforceService,
      memberService = memberService,
      subscriptionService = newSubsService,
      catalogService = newCatalogService,
      identityService = identityService,
    )
  }

  case class Resolution(
    backend: TouchpointBackend,
    typ: BackendType,
    validTestUserCredentialOpt: Option[TestUserCredentialType[_]]
  )
}

class TouchpointBackends(testUsers: TestUsers, actorSystem: ActorSystem, executionContext: ExecutionContext, wsClient: WSClient) {

  import TouchpointBackend._
  import TouchpointBackendConfig.BackendType

  implicit private val as = actorSystem
  implicit private val ec = executionContext
  implicit private val ws = wsClient

  // TestUser (especially) has to be lazy as otherwise the app can't come up without the test catalog being valid.
  lazy val Normal = TouchpointBackend(BackendType.Default)
  lazy val TestUser = TouchpointBackend(BackendType.Testing)

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
  memberService: api.MemberService,
  subscriptionService: subsv2.services.SubscriptionService[Future],
  catalogService: subsv2.services.CatalogService[Future],
  identityService: IdentityService,
) {

  lazy val catalog: Catalog = catalogService.unsafeCatalog
}
