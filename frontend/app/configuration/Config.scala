package configuration

import com.github.nscala_time.time.Imports._
import com.gu.config.{DiscountRatePlanIds, DigitalPackRatePlanIds, MembershipRatePlanIds}
import com.gu.identity.cookie.{PreProductionKeys, ProductionKeys}
import com.gu.memsub.auth.common.MemSub.Google._
import com.gu.memsub.promo._
import com.gu.salesforce.Tier
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.typesafe.config.ConfigFactory
import model.Eventbrite.EBEvent
import net.kencochrane.raven.dsn.Dsn
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scalaz.syntax.std.option._
import services._

import scala.util.Try

object Config {
  val logger = Logger(this.getClass)

  val config = ConfigFactory.load()

  lazy val siteTitle = config.getString("site.title")

  lazy val sentryDsn = Try(new Dsn(config.getString("sentry.dsn")))

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val guardianHost = config.getString("guardian.host")
  val guardianShortDomain = config.getString("guardian.shortDomain")

  val membershipUrl = config.getString("membership.url")
  val membershipHost = Uri.parse(Config.membershipUrl).host.get

  val membersDataAPIUrl = config.getString("members-data-api.url")

  val membershipFeedback = config.getString("membership.feedback")

  val idWebAppUrl = config.getString("identity.webapp.url")

  val idMember = "clientId" -> "members"

  private val idSkipConfirmation: (String, String) =
    "skipConfirmation" -> "true"

  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri") & idSkipConfirmation & idMember

  def idWebAppRegisterUrl(uri: String): String =
    (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri") & idSkipConfirmation & idMember

  def idWebAppSignOutThenInUrl(uri: String): String =
    (idWebAppUrl / "signout") ? ("returnUrl" -> idWebAppSigninUrl(uri)) & idSkipConfirmation & idMember

  def idWebAppProfileUrl = idWebAppUrl / "membership" / "edit"

  val idKeys =
    if (config.getBoolean("identity.production.keys")) new ProductionKeys
    else new PreProductionKeys

  val idApiUrl = config.getString("identity.api.url")
  val idApiClientToken = config.getString("identity.api.client.token")

  val eventbriteUrl = config.getString("eventbrite.url")

  val eventbriteApiUrl = config.getString("eventbrite.api.url")
  val eventbriteApiToken = config.getString("eventbrite.api.token")
  val eventbriteMasterclassesApiToken =
    config.getString("eventbrite.masterclasses.api.token")
  val eventbriteLocalApiToken = config.getString("eventbrite.local.api.token")
  val eventbriteApiIframeUrl = config.getString("eventbrite.api.iframe-url")
  val eventbriteRefreshTime =
    config.getInt("eventbrite.api.refresh-time-seconds")
  val eventbriteRefreshTimeForPriorityEvents =
    config.getInt("eventbrite.api.refresh-time-priority-events-seconds")
  val eventbriteWaitlistUrl = config.getString("eventbrite.waitlist.url")
  val eventbriteLimitedAvailabilityCutoff =
    config.getInt("eventbrite.limitedAvailabilityCutoff")

  def eventbriteWaitlistUrl(event: EBEvent): String =
    eventbriteWaitlistUrl ? ("eid" -> event.id) & ("tid" -> 0)

  val eventOrderingJsonUrl = config.getString("event.ordering.json")

  val facebookAppId = config.getString("facebook.app.id")

  val googleAnalyticsTrackingId =
    config.getString("google.analytics.tracking.id")

  val facebookJoinerConversionTrackingId = Tier.allPublic.map { tier =>
    tier -> config.getString(s"facebook.joiner.conversion.${tier.slug}")
  }.toMap

  val facebookEventTicketSaleTrackingId =
    config.getString("facebook.ticket.purchase")

  val googleAdwordsJoinerConversionLabel = Tier.allPublic.map { tier =>
    tier -> config.getString(s"google.adwords.joiner.conversion.${tier.slug}")
  }.toMap

  val optimizelyEnabled = config.getBoolean("optimizely.enabled")

  val corsAllowOrigin = Set(
      // identity
      "https://profile.thegulocal.com",
      "https://profile.code.dev-theguardian.com",
      "https://profile.theguardian.com",
      // theguardian.com
      "http://www.thegulocal.com",
      "http://m.code.dev-theguardian.com",
      "http://preview.gutools.co.uk",
      "https://preview.gutools.co.uk",
      "http://www.theguardian.com",
      // composer
      "https://composer.gutools.co.uk",
      "https://composer.code.dev-gutools.co.uk",
      "https://composer.qa.dev-gutools.co.uk",
      "https://composer.release.dev-gutools.co.uk",
      "https://composer.local.dev-gutools.co.uk"
  )

  val discountMultiplier = config.getDouble("event.discountMultiplier")

  val roundedDiscountPercentage: Int =
    math.round((1 - discountMultiplier.toFloat) * 100)

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == "PROD"
  val stageDev: Boolean = stage == "DEV"

  lazy val googleGroupChecker = googleGroupCheckerFor(config)

  lazy val googleAuthConfig = googleAuthConfigFor(config)

  val staffAuthorisedEmailGroups = config
    .getString("staff.authorised.emails.groups")
    .split(",")
    .map(group => s"$group@$GuardianAppsDomain")
    .toSet

  val contentApiKey = config.getString("content.api.key")

  val gridConfig = {
    val con = config.getConfig("grid.images")
    GridConfig(
        con.getString("api.url"),
        con.getString("api.key")
    )
  }

  val trackerUrl = config.getString("snowplow.url")
  val bcryptSalt = config.getString("activity.tracking.bcrypt.salt")
  val bcryptPepper = config.getString("activity.tracking.bcrypt.pepper")

  val casServiceConfig = config.getString("cas.url")
  val zuoraFreeEventTicketsAllowance =
    config.getInt("zuora.free-event-tickets-allowance")

  def membershipRatePlanIds(env: String) =
    MembershipRatePlanIds.fromConfig(config.getConfig(
            s"touchpoint.backend.environments.$env.zuora.ratePlanIds.membership"))

  def digipackRatePlanIds(env: String) =
    DigitalPackRatePlanIds.fromConfig(config.getConfig(
            s"touchpoint.backend.environments.$env.zuora.ratePlanIds.digitalpack"))

  def discountRatePlanIds(env: String): DiscountRatePlanIds =
    DiscountRatePlanIds.fromConfig(config.getConfig(
            s"touchpoint.backend.environments.$env.zuora.ratePlanIds"))

  def demoPromo(env: String) = {
    val prpIds = membershipRatePlanIds(env)
    new Promotion[Incentive](
        appliesTo = AppliesTo.ukOnly(Set(
                  prpIds.partnerMonthly,
                  prpIds.partnerYearly,
                  prpIds.patronYearly,
                  prpIds.patronMonthly
              )),
        campaignName = "English Heritage Offer - Q4 FY2016",
        codes = PromoCodeSet(PromoCode("EH2016")),
        description = "Free English Heritage membership worth £88 when you become a Partner or Patron Member",
        expires = DateTime.parse("2016-04-01T01:00:00Z"),
        imageUrl = Some(
              "https://s3-eu-west-1.amazonaws.com/memsub-promo-images/eh2016.png"),
        promotionType = Incentive(
              redemptionInstructions = "We'll send you an email with instructions on redeeming your English Heritage offer within 35 days.",
              termsAndConditions = ""
          ),
        roundelHtml = "<h1 class=\"roundel__title\">Free annual English Heritage membership</h1>\n<p class=\"roundel__description\">when you join as a Partner or Patron by 31 March</p>",
        title = "Free English Heritage membership worth £88"
    )
  }

  def discountPromo(env: String): Option[Promotion[PercentDiscount]] = {
    val prpIds = membershipRatePlanIds(env)
    new Promotion(
        appliesTo = AppliesTo.ukOnly(Set(
                  prpIds.partnerYearly
              )),
        campaignName = "Become a Partner for just £99/year",
        codes = PromoCodeSet(PromoCode("PARTNER99")),
        description = "",
        expires = DateTime.parse("2016-04-01T01:00:00Z"),
        imageUrl = None,
        promotionType = PercentDiscount(
              durationMonths = None,
              amount = 33.557046979866
          ),
        roundelHtml = "<h1 class=\"roundel__title\">Become a Partner or just £99/year</h1>\n<p class=\"roundel__description\">before 31 March 2016</p>",
        title = "Become a Partner for just £99/year"
    ).some.filter(_ => env != "PROD")
  }

  object Implicits {
    implicit val akkaSystem = Akka.system
  }
}
