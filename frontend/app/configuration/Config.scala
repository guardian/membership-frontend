package configuration

import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.membership.salesforce.Tier.{Friend, Partner, Patron, Tier}
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import model.{FriendTierPlan, PaidTierPlan}
import play.api.Logger
import services.zuora.ZuoraApiConfig
import services.{StripeCredentials, SalesforceConfig, StripeApiConfig, TouchpointBackendConfig}
import com.netaporter.uri.dsl._

object Config {
  val logger = Logger(this.getClass())

  val config = ConfigFactory.load()

  lazy val siteTitle = config.getString("site.title")

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val guardianMembershipUrl = config.getString("guardian.membership.url")
  val guardianLiveEventsTermsUrl = config.getString("guardian.live.events.terms.url")
  val guardianMasterclassesTermsUrl = config.getString("guardian.masterclasses.terms.url")
  val guardianMembershipTermsUrl = config.getString("guardian.membership.terms.url")
  val guardianPrivacyUrl = config.getString("guardian.privacy.url")
  var guardianMembershipBuildingBlogUrl = config.getString("guardian.membership.building.blog.url")
  var guardianMembershipBuildingSpaceUrl = config.getString("guardian.membership.building.space.url")

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

  val eventImageWidths = config.getList("membership.event.images.widths").unwrapped
  val eventImageRatios = config.getList("membership.event.images.ratios").unwrapped

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

  val eventOrderingJsonUrl = config.getString("event.ordering.json")

  val facebookAppId = config.getString("facebook.app.id")


  val touchpointDefaultBackend = touchpointBackendConfigFor("default")
  val touchpointTestBackend = touchpointBackendConfigFor("test")

  def touchpointBackendConfigFor(typ: String) = {
    val touchpointConfig = config.getConfig("touchpoint.backend")
    val backendEnvironmentName = touchpointConfig.getString(typ)
    val environments = touchpointConfig.getConfig("environments")

    val defaultTouchpointBackendConfig = touchpointConfigFor(environments.getConfig(backendEnvironmentName))

    logger.info(s"TouchPoint config - default-env=$typ config=${defaultTouchpointBackendConfig.hashCode}")

    defaultTouchpointBackendConfig
  }

  def touchpointConfigFor(backendConf: com.typesafe.config.Config): TouchpointBackendConfig = {
    val stripeApiConfig = StripeApiConfig(
      config.getString("stripe.api.url"), // stripe url never changes
      StripeCredentials(
        secretKey = backendConf.getString("stripe.api.key.secret"),
        publicKey = backendConf.getString("stripe.api.key.public")
      )
    )

    val salesforceConfig = SalesforceConfig(
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
        PaidTierPlan(paidTier, annual) -> backendConf.getString(s"zuora.api.${paidTier.toString.toLowerCase}.$period")
      }

      Map(paidTierPlan(false), paidTierPlan(true))
    }

    val zuoraApiConfig = ZuoraApiConfig(
      url = backendConf.getString("zuora.api.url"),
      username = backendConf.getString("zuora.api.username"),
      password = backendConf.getString("zuora.api.password"),
      Map(FriendTierPlan -> backendConf.getString(s"zuora.api.friend")) ++ plansFor(Partner) ++ plansFor(Patron)
    )

    TouchpointBackendConfig(salesforceConfig, stripeApiConfig, zuoraApiConfig)
  }

  val twitterUsername = config.getString("twitter.username")
  val twitterIphoneAppName = config.getString("twitter.app.iphone.name")
  val twitterIphoneAppId = config.getString("twitter.app.iphone.id")
  val twitterGoogleplayAppName = config.getString("twitter.app.googleplay.name")
  val twitterGoogleplayAppId = config.getString("twitter.app.googleplay.id")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val facebookJoinerConversionTrackingId = Map(
    Friend -> config.getString("facebook.joiner.conversion.friend"),
    Partner -> config.getString("facebook.joiner.conversion.partner"),
    Patron -> config.getString("facebook.joiner.conversion.patron")
  )

  val googleAdwordsJoinerConversionLabel = Map(
    Friend -> config.getString("google.adwords.joiner.conversion.friend"),
    Partner -> config.getString("google.adwords.joiner.conversion.partner"),
    Patron -> config.getString("google.adwords.joiner.conversion.patron")
  )

  val corsAllowOrigin = config.getString("cors.allow.origin")

  val discountMultiplier = config.getDouble("event.discountMultiplier")

  val stage = config.getString("stage")

  val ophanJsUrl = config.getString("ophan.js.url")

  val contentApiKey = config.getString("content.api.key")

}
