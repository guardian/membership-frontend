package wiring

import actions.{ActionRefiners, CommonActions, TouchpointActionRefiners, TouchpointCommonActions, TouchpointOAuthActions}
import com.gu.googleauth.GoogleAuthConfig
import com.gu.memsub.auth.common.MemSub.Google.googleAuthConfigFor
import play.api.routing.Router
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.BuiltInComponentsFromContext
import configuration.Config
import loghandling.Logstash
import monitoring.{HealthMonitoringTask, SentryLogging}
import play.api.cache.ehcache.EhCacheComponents
import play.api.http.HttpErrorHandler
import play.api.i18n.I18nComponents
import play.filters.csrf.CSRFComponents
import services._
import filters.{AddEC2InstanceHeader, CheckCacheHeadersFilter, Gzipper, RedirectMembersFilter}
import configuration.Config.config
import controllers._

trait AppComponents
  extends AhcWSComponents
  with EhCacheComponents
  with I18nComponents
  with AssetsComponents
  with CSRFComponents {
  self: BuiltInComponentsFromContext =>

  override lazy val httpErrorHandler: HttpErrorHandler = new monitoring.ErrorHandler(environment, configuration, sourceMapper, Some(router), executionContext)

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    new RedirectMembersFilter(),
    new CheckCacheHeadersFilter,
    csrfFilter,
    new Gzipper,
    new AddEC2InstanceHeader(wsClient)
  )

  lazy val googleAuthConfig: GoogleAuthConfig = googleAuthConfigFor(config, httpConfiguration = httpConfiguration)

  private lazy val gridService = new GridService(executionContext)
  private lazy val identityApi = new IdentityApi(wsClient, executionContext)
  private lazy val contentApiService = new GuardianContentService(actorSystem, executionContext)
  private lazy val guardianLiveEventService = new GuardianLiveEventService(executionContext, actorSystem, contentApiService, gridService)
  private lazy val masterclassEventService = new MasterclassEventService(executionContext, actorSystem, contentApiService)
  private lazy val eventbriteCollectiveServices = new EventbriteCollectiveServices(defaultCacheApi, guardianLiveEventService, masterclassEventService)
  private lazy val membersDataAPI = new MembersDataAPI(executionContext)

  private lazy val commonActionRefiners = new ActionRefiners(defaultBodyParser, executionContext)
  private lazy val commonActions = new CommonActions(defaultBodyParser, executionContext, commonActionRefiners)
  private lazy val touchpointBackends = new TouchpointBackends(actorSystem, executionContext, wsClient)
  private lazy val actionRefiners = new TouchpointActionRefiners(touchpointBackends, executionContext)
  private lazy val touchpointCommonActions = new TouchpointCommonActions(touchpointBackends, actionRefiners, defaultBodyParser, executionContext, commonActionRefiners)
  private lazy val oauthActions = new TouchpointOAuthActions(
    touchpointBackends,actionRefiners, executionContext, wsClient, defaultBodyParser, googleAuthConfig, commonActions
  )

  private lazy val joiner =  new Joiner(
    wsClient,
    identityApi,
    eventbriteCollectiveServices,
    contentApiService,
    touchpointBackends,
    oauthActions,
    actionRefiners,
    touchpointCommonActions,
    defaultBodyParser,
    executionContext,
    googleAuthConfig,
    commonActions,
    commonActionRefiners,
    membersDataAPI,
    controllerComponents
  )

  lazy val router: Router = {
    new _root_.router.Routes(
      httpErrorHandler,
      new CachedAssets(assets, executionContext, controllerComponents),
      new CacheBustedAssets(assets, controllerComponents),
      new SiteMap(commonActions, controllerComponents),
      new FrontPage(eventbriteCollectiveServices, touchpointBackends, commonActions, controllerComponents),
      new Healthcheck(eventbriteCollectiveServices, touchpointBackends, controllerComponents),
      new Testing(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions, controllerComponents),
      new FeatureOptIn(commonActions, controllerComponents),
      joiner,
      new MemberOnlyContent(contentApiService, commonActions, executionContext, controllerComponents),
      new Login(commonActions, controllerComponents),
      new StaffAuth(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions, controllerComponents),
      new OAuth(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions, controllerComponents),
      new Outages(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions, controllerComponents),
      new Staff(wsClient, eventbriteCollectiveServices, defaultBodyParser, executionContext, googleAuthConfig, commonActions, controllerComponents),
      new SubscriptionController(touchpointCommonActions, touchpointBackends, executionContext, controllerComponents),
      new WhatsOn(eventbriteCollectiveServices, touchpointBackends, commonActions, executionContext, controllerComponents),
      new rest.EventApi(eventbriteCollectiveServices, commonActions, controllerComponents),
      new Event(wsClient, eventbriteCollectiveServices, touchpointBackends, actionRefiners, touchpointCommonActions, defaultBodyParser, executionContext, googleAuthConfig, commonActions, commonActionRefiners, controllerComponents),
      new TierController(joiner, identityApi, touchpointCommonActions, touchpointBackends, commonActions, executionContext, controllerComponents),
      new Info(identityApi, contentApiService, touchpointBackends, commonActions, commonActionRefiners, executionContext, controllerComponents),
      new Redirects(commonActions, controllerComponents),
      new Bundle(touchpointBackends, commonActions, controllerComponents),
      new PatternLibrary(eventbriteCollectiveServices, touchpointBackends, commonActions, controllerComponents),
      new User(identityApi, touchpointCommonActions, executionContext, commonActions, membersDataAPI, controllerComponents),
      new VanityUrl(commonActions, controllerComponents),
      new PricingApi(touchpointBackends, commonActions, controllerComponents),
      new Giraffe(commonActions, controllerComponents),
      new MembershipStatus(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions, controllerComponents),
      new PayPal(touchpointBackends, executionContext, commonActions, controllerComponents)
    )
  }


  HealthMonitoringTask.start(actorSystem, executionContext, Config.stage, Config.appName)
  SentryLogging.init()
  Logstash.init(Config)
  guardianLiveEventService.start()
  masterclassEventService.start()
  contentApiService.start()
}
