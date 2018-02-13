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
import services.{GuardianContentService, GuardianLiveEventService, MasterclassEventService}
import services.{EventbriteCollectiveServices, IdentityApi, TouchpointBackends}
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

  override lazy val httpErrorHandler: HttpErrorHandler = new monitoring.ErrorHandler(environment, configuration, sourceMapper, Some(router))

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    new RedirectMembersFilter(),
    new CheckCacheHeadersFilter,
    csrfFilter,
    new Gzipper,
    new AddEC2InstanceHeader(wsClient)
  )

  lazy val googleAuthConfig: GoogleAuthConfig = googleAuthConfigFor(config, httpConfiguration = httpConfiguration)

  private lazy val identityApi = new IdentityApi(wsClient)
  private lazy val contentApiService = new GuardianContentService(actorSystem)
  private lazy val guardianLiveEventService = new GuardianLiveEventService(executionContext, actorSystem, contentApiService)
  private lazy val masterclassEventService = new MasterclassEventService(executionContext, actorSystem, contentApiService)
  private lazy val eventbriteCollectiveServices = new EventbriteCollectiveServices(defaultCacheApi, guardianLiveEventService, masterclassEventService)

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
    messagesApi,
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
    commonActionRefiners
  )

  lazy val router: Router = {
    new _root_.router.Routes(
      httpErrorHandler,
      new CachedAssets(assets),
      new CacheBustedAssets(assets),
      new SiteMap(commonActions),
      new FrontPage(eventbriteCollectiveServices, touchpointBackends, commonActions),
      new Healthcheck(eventbriteCollectiveServices, touchpointBackends),
      new Testing(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions),
      new FeatureOptIn(commonActions),
      joiner,
      new MemberOnlyContent(contentApiService, commonActions),
      new Login(commonActions),
      new StaffAuth(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions),
      new OAuth(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions),
      new Outages(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions),
      new Staff(wsClient, eventbriteCollectiveServices, defaultBodyParser, executionContext, googleAuthConfig, commonActions),
      new SubscriptionController(touchpointCommonActions, touchpointBackends),
      new WhatsOn(eventbriteCollectiveServices, touchpointBackends, commonActions),
      new rest.EventApi(eventbriteCollectiveServices, commonActions),
      new Event(wsClient, eventbriteCollectiveServices, touchpointBackends, actionRefiners, touchpointCommonActions, defaultBodyParser, executionContext, googleAuthConfig, commonActions, commonActionRefiners),
      new TierController(joiner, identityApi, touchpointCommonActions, touchpointBackends, commonActions),
      new Info(identityApi, contentApiService, touchpointBackends, commonActions, commonActionRefiners),
      new Redirects(commonActions),
      new Bundle(touchpointBackends, commonActions),
      new PatternLibrary(eventbriteCollectiveServices, touchpointBackends, commonActions),
      new User(identityApi, touchpointCommonActions, executionContext, commonActions),
      new VanityUrl(commonActions),
      new PricingApi(touchpointBackends, commonActions),
      new Giraffe(commonActions),
      new MembershipStatus(wsClient, defaultBodyParser, executionContext, googleAuthConfig, commonActions),
      new PayPal(touchpointBackends, executionContext, commonActions)
    )
  }


  HealthMonitoringTask.start(actorSystem, executionContext, Config.stage, Config.appName)
  SentryLogging.init()
  Logstash.init(Config)
  guardianLiveEventService.start()
  masterclassEventService.start()
  contentApiService.start()
}
