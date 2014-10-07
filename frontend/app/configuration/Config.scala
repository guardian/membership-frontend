package configuration

import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.membership.salesforce.Tier.{Partner, Patron, Tier}
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import model.{FriendTierPlan, PaidTierPlan, TierPlan}
import services.StripeApiConfig
import services.zuora.ZuoraApiConfig

object Config {
  val config = ConfigFactory.load()

  lazy val siteTitle = config.getString("site.title")

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val guardianUrl = config.getString("guardian.url")
  val guardianLiveEventsTermsUrl = config.getString("guardian.live.events.terms.url")
  val guardianMembershipTermsUrl = config.getString("guardian.membership.terms.url")
  val guardianPrivacyUrl = config.getString("guardian.privacy.url")
  var guardianMembershipBuildingBlogUrl = config.getString("guardian.membership.building.blog.url")
  var guardianMembershipBuildingSpaceUrl = config.getString("guardian.membership.building.space.url")

  val membershipUrl = config.getString("membership.url")
  val membershipFeedback = config.getString("membership.feedback")

  val idWebAppUrl = config.getString("identity.webapp.url")

  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri")

  def idWebAppRegisterUrl(uri: String): String =
    (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri")

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
  val eventbriteApiEventListUrl = config.getString("eventbrite.api.event-list-url")
  val eventbriteApiIframeUrl = config.getString("eventbrite.api.iframe-url")
  val eventbriteRefreshTimeForAllEvents = config.getInt("eventbrite.api.refresh-time-all-events-seconds")
  val eventbriteRefreshTimeForPriorityEvents = config.getInt("eventbrite.api.refresh-time-priority-events-seconds")

  val eventOrderingJsonUrl = config.getString("event.ordering.json")

  val stripeApiConfig = StripeApiConfig(
    url = config.getString("stripe.api.url"),
    secretKey = config.getString("stripe.api.key.secret"),
    publicKey = config.getString("stripe.api.key.public")
  )

  val salesforceConsumerKey = config.getString("salesforce.consumer.key")
  val salesforceConsumerSecret = config.getString("salesforce.consumer.secret")
  val salesforceApiUrl = config.getString("salesforce.api.url")
  val salesforceApiUsername = config.getString("salesforce.api.username")
  val salesforceApiPassword = config.getString("salesforce.api.password")
  val salesforceApiToken = config.getString("salesforce.api.token")

  val zuoraApiConfig = ZuoraApiConfig(
    url = config.getString("zuora.api.url"),
    username = config.getString("zuora.api.username"),
    password = config.getString("zuora.api.password")
  )

  def plansFor(paidTier: Tier) = {
    def paidTierPlan(annual: Boolean) = {
      val period = if (annual) "annual" else "monthly"
      PaidTierPlan(paidTier, annual) -> config.getString(s"zuora.api.${paidTier.toString.toLowerCase}.$period")
    }

    Map (paidTierPlan(false), paidTierPlan(true))
  }

  val tierRatePlanIds: Map[TierPlan, String] =
    Map(FriendTierPlan -> config.getString(s"zuora.api.friend")) ++ plansFor(Partner) ++ plansFor(Patron)

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val corsAllowOrigin = config.getString("cors.allow.origin")

  val discountMultiplier = config.getDouble("event.discountMultiplier")

  val stage = config.getString("stage")

  val ophanJsUrl = config.getString("ophan.js.url")

}
