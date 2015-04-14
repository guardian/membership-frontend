package configuration

import com.gu.googleauth.{GoogleAuthConfig, GoogleGroupConfig}
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.membership.salesforce.Tier
import com.gu.membership.stripe.{StripeCredentials, StripeApiConfig}
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

  val membershipUrl = config.getString("membership.url")
  val membershipFeedback = config.getString("membership.feedback")

  val idWebAppUrl = config.getString("identity.webapp.url")

  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri") ? ("skipConfirmation" -> "true")

  def idWebAppRegisterUrl(uri: String): String =
    (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri") ? ("skipConfirmation" -> "true")

  def idWebAppSignOutThenInUrl(uri: String): String =
    (idWebAppUrl / "signout") ? ("returnUrl" -> idWebAppSigninUrl(uri)) ? ("skipConfirmation" -> "true")

  def eventImageUrlPath(id: String): String =
    config.getString("membership.event.images.url") + id

  val idKeys = if (config.getBoolean("identity.production.keys")) new ProductionKeys else new PreProductionKeys

  val idApiUrl = config.getString("identity.api.url")
  val idApiClientToken = config.getString("identity.api.client.token")

  val eventbriteUrl = config.getString("eventbrite.url")

  val eventbriteApiUrl = config.getString("eventbrite.api.url")
  val eventbriteApiToken = config.getString("eventbrite.api.token")
  val eventbriteMasterclassesApiToken = config.getString("eventbrite.masterclasses.api.token")
  val eventbriteLocalApiToken = config.getString("eventbrite.local.api.token")
  val eventbriteApiIframeUrl = config.getString("eventbrite.api.iframe-url")
  val eventbriteRefreshTime = config.getInt("eventbrite.api.refresh-time-seconds")
  val eventbriteRefreshTimeForPriorityEvents = config.getInt("eventbrite.api.refresh-time-priority-events-seconds")
  val eventbriteWaitlistUrl = config.getString("eventbrite.waitlist.url")
  val eventbriteLimitedAvailabilityCutoff = config.getInt("eventbrite.limitedAvailabilityCutoff")

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

    def plansForTier(paidTier: Tier) = {
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
      productRatePlans = Map(
        FriendTierPlan -> backendConf.getString(s"zuora.api.friend"),
        StaffPlan -> backendConf.getString(s"zuora.api.staff")
      ) ++ Seq(Tier.Supporter, Tier.Partner, Tier.Patron).map(plansForTier).reduce(_ ++ _)
    )

    TouchpointBackendConfig(salesforceConfig, stripeApiConfig, zuoraApiConfig)
  }

  val twitterUsername = config.getString("twitter.username")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val facebookJoinerConversionTrackingId =
    Tier.allPublic.map { tier => tier -> config.getString(s"facebook.joiner.conversion.${tier.slug}") }.toMap

  val facebookEventTicketSaleTrackingId = config.getString("facebook.ticket.purchase")

  val googleAdwordsJoinerConversionLabel =
    Tier.allPublic.map { tier => tier -> config.getString(s"google.adwords.joiner.conversion.${tier.slug}") }.toMap

  val corsAllowOrigin: Set[String] = config.getString("cors.allow.origin").split(",").toSet

  val discountMultiplier = config.getDouble("event.discountMultiplier")

  val roundedDiscountPercentage: Int = math.round((1-discountMultiplier.toFloat)*100)

  val stage = config.getString("stage")

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

  val trackerUrl = config.getString("snowplow.url")
  val bcryptSalt = config.getString("activity.tracking.bcrypt.salt")
  val bcryptPepper = config.getString("activity.tracking.bcrypt.pepper")

  val casServiceConfig = config.getString("cas.url")

}
