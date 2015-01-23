package configuration

import com.gu.googleauth.{GoogleAuthConfig, GoogleGroupConfig}
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.membership.salesforce.Tier
import com.typesafe.config.ConfigFactory
import model.Eventbrite.EBEvent
import model.{StaffPlan, FriendTierPlan, PaidTierPlan}
import net.kencochrane.raven.dsn.Dsn
import play.api.Logger
import services.zuora.ZuoraApiConfig
import services._
import com.netaporter.uri.dsl._
import scala.util.Try

object Config {
  val logger = Logger(this.getClass())

  val config = ConfigFactory.load()

  lazy val siteTitle = config.getString("site.title")

  lazy val sentryDsn = Try(new Dsn(config.getString("sentry.dsn")))

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val guardianMembershipUrl = config.getString("guardian.membership.url")
  val guardianLiveEventsTermsUrl = config.getString("guardian.live.events.terms.url")
  val guardianMasterclassesTermsUrl = config.getString("guardian.masterclasses.terms.url")
  val guardianMembershipTermsUrl = config.getString("guardian.membership.terms.url")
  val guardianPrivacyUrl = config.getString("guardian.privacy.url")
  var guardianMembershipBuildingBlogUrl = config.getString("guardian.membership.building.blog.url")
  var guardianMembershipBuildingSpaceUrl = config.getString("guardian.membership.building.space.url")
  val guardianContactUsUrl = config.getString("guardian.membership.contact.us.url")

  val membershipUrl = config.getString("membership.url")
  val membershipFeedback = config.getString("membership.feedback")
  val membershipSupport = config.getString("membership.support")
  val membershipSupportStaffEmail = config.getString("membership.staff.email")
  val membershipDevEmail = config.getString("membership.dev.email")

  val idWebAppUrl = config.getString("identity.webapp.url")

  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri") ? ("skipConfirmation" -> "true")

  def idWebAppRegisterUrl(uri: String): String =
    (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri") ? ("skipConfirmation" -> "true")

  def idWebAppSignOutThenInUrl(uri: String): String =
    (idWebAppUrl / "signout") ? ("returnUrl" -> idWebAppSigninUrl(uri)) ? ("skipConfirmation" -> "true")

  def eventImageUrlPath(id: String): String =
    config.getString("membership.event.images.url") + id

  val eventImageWidths = config.getList("membership.event.images.widths").unwrapped
  val eventImageRatios = config.getList("membership.event.images.ratios").unwrapped
  val homeImageWidths = config.getList("membership.home.images.widths").unwrapped
  val homeImageRatios = config.getList("membership.home.images.ratios").unwrapped

  val idKeys = if (config.getBoolean("identity.production.keys")) new ProductionKeys else new PreProductionKeys

  val idApiUrl = config.getString("identity.api.url")
  val idApiClientToken = config.getString("identity.api.client.token")

  val eventbriteUrl = config.getString("eventbrite.url")

  val eventbriteApiUrl = config.getString("eventbrite.api.url")
  val eventbriteApiToken = config.getString("eventbrite.api.token")
  val eventbriteMasterclassesApiToken = config.getString("eventbrite.masterclasses.api.token")
  val eventbriteApiIframeUrl = config.getString("eventbrite.api.iframe-url")
  val eventbriteRefreshTime = config.getInt("eventbrite.api.refresh-time-seconds")
  val eventbriteRefreshTimeForPriorityEvents = config.getInt("eventbrite.api.refresh-time-priority-events-seconds")
  val eventbriteWaitlistUrl = config.getString("eventbrite.waitlist.url")

  def eventbriteWaitlistUrl(event: EBEvent): String =
    eventbriteWaitlistUrl ? ("eid" -> event.id) & ("tid" -> 0)

  val eventOrderingJsonUrl = config.getString("event.ordering.json")

  val facebookAppId = config.getString("facebook.app.id")


  val touchpointDefaultBackend = touchpointBackendConfigFor("default")
  val touchpointTestBackend = touchpointBackendConfigFor("test")

  def touchpointBackendConfigFor(typ: String) = {
    val touchpointConfig = config.getConfig("touchpoint.backend")
    val backendEnvironmentName = touchpointConfig.getString(typ)
    val environments = touchpointConfig.getConfig("environments")

    val defaultTouchpointBackendConfig = touchpointConfigFor(environments, backendEnvironmentName)

    logger.info(s"TouchPoint config - default-env=$typ config=${defaultTouchpointBackendConfig.hashCode}")

    defaultTouchpointBackendConfig
  }

  def touchpointConfigFor(environmentsConf: com.typesafe.config.Config, environmentName: String): TouchpointBackendConfig = {

    val backendConf: com.typesafe.config.Config = environmentsConf.getConfig(environmentName)

    val stripeApiConfig = StripeApiConfig(
      environmentName,
      config.getString("stripe.api.url"), // stripe url never changes
      StripeCredentials(
        secretKey = backendConf.getString("stripe.api.key.secret"),
        publicKey = backendConf.getString("stripe.api.key.public")
      )
    )

    val salesforceConfig = SalesforceConfig(
      environmentName,
      consumerKey = backendConf.getString("salesforce.consumer.key"),
      consumerSecret = backendConf.getString("salesforce.consumer.secret"),
      apiURL = backendConf.getString("salesforce.api.url"),
      apiUsername = backendConf.getString("salesforce.api.username"),
      apiPassword = backendConf.getString("salesforce.api.password"),
      apiToken = backendConf.getString("salesforce.api.token")
    )

    def plansFor(paidTier: Tier) = {
      def paidTierPlan(annual: Boolean) = {
        val period = if (annual) "annual" else "monthly"
        PaidTierPlan(paidTier, annual) -> backendConf.getString(s"zuora.api.${paidTier.slug}.$period")
      }

      Map(paidTierPlan(false), paidTierPlan(true))
    }

    val zuoraApiConfig = ZuoraApiConfig(
      environmentName,
      backendConf.getString("zuora.api.url"),
      username = backendConf.getString("zuora.api.username"),
      password = backendConf.getString("zuora.api.password"),
        Map(FriendTierPlan -> backendConf.getString(s"zuora.api.friend"),
          StaffPlan -> backendConf.getString(s"zuora.api.staff")) ++
          plansFor(Tier.Partner) ++ plansFor(Tier.Patron)
      )

    TouchpointBackendConfig(salesforceConfig, stripeApiConfig, zuoraApiConfig)
  }

  val twitterUsername = config.getString("twitter.username")
  val twitterIphoneAppName = config.getString("twitter.app.iphone.name")
  val twitterIphoneAppId = config.getString("twitter.app.iphone.id")
  val twitterGoogleplayAppName = config.getString("twitter.app.googleplay.name")
  val twitterGoogleplayAppId = config.getString("twitter.app.googleplay.id")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val facebookJoinerConversionTrackingId = Map[Tier, String](
    Tier.Friend -> config.getString("facebook.joiner.conversion.friend"),
    Tier.Partner -> config.getString("facebook.joiner.conversion.partner"),
    Tier.Patron -> config.getString("facebook.joiner.conversion.patron")
  )

  val facebookEventTicketSaleTrackingId = config.getString("facebook.ticket.purchase")

  val googleAdwordsJoinerConversionLabel = Map[Tier, String](
    Tier.Friend -> config.getString("google.adwords.joiner.conversion.friend"),
    Tier.Partner -> config.getString("google.adwords.joiner.conversion.partner"),
    Tier.Patron -> config.getString("google.adwords.joiner.conversion.patron")
  )

  val corsAllowOrigin = config.getString("cors.allow.origin")

  val discountMultiplier = config.getDouble("event.discountMultiplier")

  val stage = config.getString("stage")

  val ophanJsUrl = config.getString("ophan.js.url")

  val googleAuthConfig = {
    val con = config.getConfig("google.oauth")
    GoogleAuthConfig(
      con.getString("client.id"),
      con.getString("client.secret"),
      con.getString("callback"),
      Some("guardian.co.uk")        // Google App domain to restrict login
    )
  }

  val googleGroupCheckerAuthConfig = {
    val con = config.getConfig("google.groups")
    GoogleGroupConfig(
      con.getString("client.username"),
      con.getString("client.password"),
      "guardian.co.uk",
      ""
    )
  }

  val staffAuthorisedEmailGroups = config.getString("staff.authorised.emails.groups").split(",").toSet

  val contentApiKey = config.getString("content.api.key")

  val gridConfig = {
    val con = config.getConfig("grid.images")
    GridConfig(
      con.getString("media.url"),
      con.getString("api.url"),
      con.getString("api.key")
    )
  }

}
