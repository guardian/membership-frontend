package wiring

import actions.TouchpointOAuthActions
import play.api.routing.Router
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.api.BuiltInComponentsFromContext
import com.softwaremill.macwire.wire
import configuration.Config
import loghandling.Logstash
import monitoring.{HealthMonitoringTask, SentryLogging}
import play.api.cache.EhCacheComponents
import play.api.http.HttpErrorHandler
import play.api.i18n.I18nComponents
import play.filters.csrf.CSRFComponents
import services.TouchpointBackendProvider

trait AppComponents
  extends AhcWSComponents
  with EhCacheComponents
  with I18nComponents
  with CSRFComponents {
  self: BuiltInComponentsFromContext =>

  private lazy val ec = actorSystem.dispatcher

  override lazy val httpErrorHandler: HttpErrorHandler = new monitoring.ErrorHandler(environment, configuration, sourceMapper, Some(router))

  private lazy val redirectMembersFilter = wire[filters.RedirectMembersFilter]
  private lazy val checkCacheHeadersFilter = wire[filters.CheckCacheHeadersFilter]
  private lazy val gzipper = wire[filters.Gzipper]
  private lazy val addEC2InstanceHeader = new filters.AddEC2InstanceHeader(materializer, wsClient)

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(
    redirectMembersFilter,
    checkCacheHeadersFilter,
    csrfFilter,
    gzipper,
    addEC2InstanceHeader
  )

  private lazy val identityApi = wire[services.IdentityApi]
  private lazy val guardianContentService = new services.GuardianContentService()(actorSystem)
  private lazy val guardianLiveEventService = new services.GuardianLiveEventService()(actorSystem.dispatcher, actorSystem, guardianContentService)
  private lazy val masterclassEventService = new services.MasterclassEventService()(actorSystem.dispatcher, actorSystem, guardianContentService)
  private lazy val eventbriteCollectiveServices = new services.EventbriteCollectiveServices(defaultCacheApi, guardianLiveEventService, masterclassEventService)

  private lazy val touchpointBackendProvider = new TouchpointBackendProvider()(actorSystem, actorSystem.dispatcher)
  private lazy val touchpointActionRefiners = new actions.TouchpointActionRefiners()(touchpointBackendProvider, actorSystem.dispatcher)
  private lazy val touchpointCommonActions = new actions.TouchpointCommonActions(touchpointBackendProvider, touchpointActionRefiners)
  private lazy val touchpointOAuthActions = new TouchpointOAuthActions(
    touchpointBackendProvider,touchpointActionRefiners, actorSystem.dispatcher, wsClient
  )

  private lazy val bundle = wire[controllers.Bundle]
  private lazy val cacheBustedAssets = wire[controllers.CacheBustedAssets]
  private lazy val cachedAssets = wire[controllers.CachedAssets]
  private lazy val event = wire[controllers.Event]
  private lazy val featureOptIn = wire[controllers.FeatureOptIn]
  private lazy val frontPage = wire[controllers.FrontPage]
  private lazy val giraffe = wire[controllers.Giraffe]
  private lazy val healthcheck = wire[controllers.Healthcheck]
  private lazy val info = wire[controllers.Info]
  private lazy val joiner = wire[controllers.Joiner]
  private lazy val login = wire[controllers.Login]
  private lazy val memberOnlyContent = wire[controllers.MemberOnlyContent]
  private lazy val membershipStatus = wire[controllers.MembershipStatus]
  private lazy val oAuth = wire[controllers.OAuth]
  private lazy val outages = wire[controllers.Outages]
  private lazy val patternLibrary = wire[controllers.PatternLibrary]
  private lazy val payPal = wire[controllers.PayPal]
  private lazy val pricingApi = wire[controllers.PricingApi]
  private lazy val redirects = wire[controllers.Redirects]
  private lazy val siteMap = wire[controllers.SiteMap]
  private lazy val staff = wire[controllers.Staff]
  private lazy val staffAuth = wire[controllers.StaffAuth]
  private lazy val subscription = wire[controllers.Subscription]
  private lazy val testing = wire[controllers.Testing]
  private lazy val tierController = wire[controllers.TierController]
  private lazy val user = wire[controllers.User]
  private lazy val vanityUrl = wire[controllers.VanityUrl]
  private lazy val whatsOn = wire[controllers.WhatsOn]
  private lazy val eventApi = wire[controllers.rest.EventApi]

  lazy val router: Router = {
    val prefix = "/"
    wire[_root_.router.Routes]
  }

  HealthMonitoringTask.start(actorSystem, actorSystem.dispatcher, Config.stage, Config.appName)
  SentryLogging.init()
  Logstash.init(Config)
  guardianLiveEventService.start()
  masterclassEventService.start()
  guardianContentService.start()
}
