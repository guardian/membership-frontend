package configuration
import com.gu.config._
import com.gu.i18n.Country
import com.gu.memsub.auth.common.MemSub.Google._
import com.gu.salesforce.Tier
import com.gu.zuora.api.{InvoiceTemplate, InvoiceTemplates}
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.dsl._
import com.typesafe.config.ConfigFactory
import model.Eventbrite.EBEvent
import services._

import scala.util.Try

object Config {

  val config = ConfigFactory.load()

  val appName = "membership-frontend"

  lazy val siteTitle = config.getString("site.title")

  lazy val sentryDsn = Try(config.getString("sentry.dsn"))

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val guardianHost = config.getString("guardian.host")
  val guardianShortDomain = config.getString("guardian.shortDomain")

  val membershipUrl = config.getString("membership.url")
  val membershipSupporterUrl = config.getString("membership.supporter.url")
  val membershipHost = Uri.parse(Config.membershipUrl).toUrl.hostOption.get

  val membersDataAPIUrl = config.getString("members-data-api.url")

  val membershipFeedback = config.getString("membership.feedback")

  val idWebAppUrl = config.getString("identity.webapp.url")

  val idMember = "clientId" -> "members"

  val sendJVMMetrics = Try { config.getBoolean("send-jvm-metrics") }.toOption.contains(true)

  private val idSkipConfirmation: (String, String) = "skipConfirmation" -> "true"

  def googleSigninUrl = oauthWebAppSigninUrl("google")(_)
  def facebookSigninUrl = oauthWebAppSigninUrl("facebook")(_)

  private def oauthWebAppSigninUrl(socialProvider: String)(uri: String): String =
    ("https://oauth.theguardian.com" / socialProvider / "signin") ? ("returnUrl" -> s"$membershipUrl$uri") & idSkipConfirmation & idMember

  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri") & idSkipConfirmation & idMember

  def idWebAppRegisterUrl(uri: String): String =
    (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri") & idSkipConfirmation & idMember

  def idWebAppSignOutThenRegisterUrl(uri: String): String =
    (idWebAppUrl / "signout") ? ("returnUrl" -> (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri" & idSkipConfirmation & idMember))

  def idWebAppProfileUrl =
    idWebAppUrl / "membership"/ "edit"

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
  val eventbriteLimitedAvailabilityCutoff = config.getInt("eventbrite.limitedAvailabilityCutoff")

  def eventbriteWaitlistUrl(event: EBEvent): String =
    eventbriteWaitlistUrl ? ("eid" -> event.id) & ("tid" -> 0)

  val eventOrderingJsonUrl = config.getString("event.ordering.json")

  val facebookAppId = config.getString("facebook.app.id")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val googleAdwordsJoinerConversionLabel =
    Tier.allPublic.map { tier => tier -> config.getString(s"google.adwords.joiner.conversion.${tier.slug}") }.toMap

  val optimizelyEnabled = config.getBoolean("optimizely.enabled")

  val corsAllowOrigin = Set(
    // identity
    "https://profile.thegulocal.com",
    "https://profile.code.dev-theguardian.com",
    "https://profile.theguardian.com",
    // theguardian.com
    "http://www.thegulocal.com",
    "https://www.thegulocal.com",
    "http://m.code.dev-theguardian.com",
    "https://m.code.dev-theguardian.com",
    "http://preview.gutools.co.uk",
    "https://preview.gutools.co.uk",
    "http://www.theguardian.com",
    "https://www.theguardian.com",
    // composer
    "https://composer.gutools.co.uk",
    "https://composer.code.dev-gutools.co.uk",
    "https://composer.qa.dev-gutools.co.uk",
    "https://composer.release.dev-gutools.co.uk",
    "https://composer.local.dev-gutools.co.uk"
  )

  val discountMultiplier = config.getDouble("event.discountMultiplier")

  val roundedDiscountPercentage: Int = math.round((1-discountMultiplier.toFloat)*100)

  val stage = config.getString("stage")
  val stageProd: Boolean = stage == Stages.PROD
  val stageDev: Boolean = stage == Stages.DEV

  lazy val googleGroupChecker = googleGroupCheckerFor(config)

  val staffAuthorisedEmailGroups = config.getString("staff.authorised.emails.groups").split(",").map(group => s"$group@$GuardianAppsDomain").toSet

  val contentApiKey = config.getString("content.api.key")

  val gridConfig = {
    val con = config.getConfig("grid.images")
    GridConfig(
      con.getString("api.url"),
      con.getString("api.key")
    )
  }

  val casServiceConfig = config.getString("cas.url")
  val zuoraFreeEventTicketsAllowance = config.getInt("zuora.free-event-tickets-allowance")

  def productIds(env: String): com.gu.memsub.subsv2.reads.ChargeListReads.ProductIds =
    SubsV2ProductIds(config.getConfig(s"touchpoint.backend.environments.$env.zuora.productIds"))

  def invoiceTemplateOverrides(env: String): Map[Country, InvoiceTemplate] =
    InvoiceTemplates.fromConfig(config.getConfig(s"touchpoint.backend.environments.$env.zuora.invoiceTemplateIds")).map(it => (it.country, it)).toMap

  object Logstash {
    private val param = Try{config.getConfig("param.logstash")}.toOption
    val stream = Try{param.map(_.getString("stream"))}.toOption.flatten
    val streamRegion = Try{param.map(_.getString("streamRegion"))}.toOption.flatten
    val enabled = Try{config.getBoolean("logstash.enabled")}.toOption.contains(true)
  }

}
