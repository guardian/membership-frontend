package wiring

import actions.{TouchpointActionRefiners, TouchpointCommonActions, TouchpointOAuthActions}
import play.api.routing.Router
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.BuiltInComponentsFromContext
import configuration.Config
import loghandling.Logstash
import monitoring.{HealthMonitoringTask, SentryLogging}
import play.api.cache.EhCacheComponents
import play.api.http.HttpErrorHandler
import play.api.i18n.I18nComponents
import play.filters.csrf.CSRFComponents
import services.{GuardianContentService, GuardianLiveEventService, MasterclassEventService}
import services.{IdentityApi, EventbriteCollectiveServices, TouchpointBackends}
import filters.{AddEC2InstanceHeader, CheckCacheHeadersFilter, Gzipper, RedirectMembersFilter}
import controllers._

trait AppComponents
  extends AhcWSComponents
  with EhCacheComponents
  with I18nComponents
  with CSRFComponents {
  self: BuiltInComponentsFromContext =>

  private val executionContext = actorSystem.dispatcher

  override lazy val httpErrorHandler: HttpErrorHandler = new monitoring.ErrorHandler(environment, configuration, sourceMapper, Some(router))

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    new RedirectMembersFilter(),
    new CheckCacheHeadersFilter,
    csrfFilter,
    new Gzipper,
    new AddEC2InstanceHeader(wsClient)
  )

  private lazy val identityApi = new IdentityApi(wsClient)
  private lazy val contentApiService = new GuardianContentService(actorSystem)
  private lazy val guardianLiveEventService = new GuardianLiveEventService(executionContext, actorSystem, contentApiService)
  private lazy val masterclassEventService = new MasterclassEventService(executionContext, actorSystem, contentApiService)
  private lazy val eventbriteCollectiveServices = new EventbriteCollectiveServices(defaultCacheApi, guardianLiveEventService, masterclassEventService)

  private lazy val touchpointBackends = new TouchpointBackends(actorSystem, executionContext, wsClient)
  private lazy val actionRefiners = new TouchpointActionRefiners(touchpointBackends, executionContext)
  private lazy val commonActions = new TouchpointCommonActions(touchpointBackends, actionRefiners)
  private lazy val oauthActions = new TouchpointOAuthActions(
    touchpointBackends,actionRefiners, executionContext, wsClient
  )

  private lazy val joiner =  new Joiner(
    wsClient,
    messagesApi,
    identityApi,
    eventbriteCollectiveServices,
    contentApiService,
    touchpointBackends,
    oauthActions,
    actionRefiners,
    commonActions
  )

  lazy val router: Router = {
    new _root_.router.Routes(
      httpErrorHandler,
      new CachedAssets(),
      new CacheBustedAssets(),
      new SiteMap(),
      new FrontPage(eventbriteCollectiveServices, touchpointBackends),
      new Healthcheck(eventbriteCollectiveServices, touchpointBackends),
      new Testing(wsClient),
      new FeatureOptIn(),
      joiner,
      new MemberOnlyContent(contentApiService),
      new Login(),
      new StaffAuth(wsClient),
      new OAuth(wsClient),
      new Outages(wsClient),
      new Staff(wsClient, eventbriteCollectiveServices),
      new Subscription(commonActions, touchpointBackends),
      new WhatsOn(eventbriteCollectiveServices, touchpointBackends),
      new rest.EventApi(eventbriteCollectiveServices),
      new Event(wsClient, eventbriteCollectiveServices, touchpointBackends, actionRefiners, commonActions),
      new TierController(joiner, identityApi, commonActions, touchpointBackends),
      new Info(identityApi, contentApiService, touchpointBackends),
      new Redirects(),
      new Bundle(touchpointBackends),
      new PatternLibrary(eventbriteCollectiveServices, touchpointBackends),
      new User(identityApi, commonActions, executionContext),
      new VanityUrl(),
      new PricingApi(touchpointBackends),
      new Giraffe(),
      new MembershipStatus(wsClient),
      new PayPal(touchpointBackends, executionContext)
    )
  }


  HealthMonitoringTask.start(actorSystem, executionContext, Config.stage, Config.appName)
  SentryLogging.init()
  Logstash.init(Config)
  guardianLiveEventService.start()
  masterclassEventService.start()
  contentApiService.start()
}
