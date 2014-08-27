package configuration

import com.netaporter.uri.dsl._

import com.typesafe.config.ConfigFactory
import com.gu.identity.cookie.{ PreProductionKeys, ProductionKeys }
import com.gu.membership.salesforce.Tier

import collection.convert.wrapAsScala._

object Config {
  val config = ConfigFactory.load()

  lazy val siteTitle = config.getString("site.title")

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val membershipUrl = config.getString("membership.url")
  val membershipDebug = config.getBoolean("membership.debug")

  val idWebAppUrl = config.getString("identity.webapp.url")

  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri")

  def idWebAppRegisterUrl(uri: String): String =
    (idWebAppUrl / "register") ? ("returnUrl" -> s"$membershipUrl$uri")

  def eventImageUrlPath(id: String): String =
    config.getString("membership.event.images.url") + id

  val idKeys = if (config.getBoolean("identity.production.keys")) new ProductionKeys else new PreProductionKeys

  val idApiUrl = config.getString("identity.api.url")
  val idApiClientToken = config.getString("identity.api.client.token")

  val eventbriteApiUrl = config.getString("eventbrite.api.url")
  val eventbriteApiToken = config.getString("eventbrite.api.token")
  val eventbriteApiEventListUrl = config.getString("eventbrite.api.event-list-url")
  val eventbriteApiIframeUrl = config.getString("eventbrite.api.iframe-url")

  val eventOrderingJsonUrl = config.getString("event.ordering.json")

  val stripeApiURL = config.getString("stripe.api.url")
  val stripeApiSecret = config.getString("stripe.api.secret")

  val salesforceConsumerKey = config.getString("salesforce.consumer.key")
  val salesforceConsumerSecret = config.getString("salesforce.consumer.secret")
  val salesforceApiUrl = config.getString("salesforce.api.url")
  val salesforceApiUsername = config.getString("salesforce.api.username")
  val salesforceApiPassword = config.getString("salesforce.api.password")
  val salesforceApiToken = config.getString("salesforce.api.token")

  val zuoraApiUrl = config.getString("zuora.api.url")
  val zuoraApiUsername = config.getString("zuora.api.username")
  val zuoraApiPassword = config.getString("zuora.api.password")

  val zuoraApiFriend = config.getString("zuora.api.friend")
  val zuoraApiPartnerMonthly = config.getString("zuora.api.partner.monthly")
  val zuoraApiPartnerAnnual = config.getString("zuora.api.partner.annual")
  val zuoraApiPatronMonthly = config.getString("zuora.api.patron.monthly")
  val zuoraApiPatronAnnual = config.getString("zuora.api.patron.annual")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val corsAllowOrigin = config.getString("cors.allow.origin")

  val discountMultiplier = config.getDouble("event.discountMultiplier")


  case class Benefits(leadin: String, list: Seq[String], priceText: String, cta: String)

  val benefits: Map[Tier.Value, Benefits] = Tier.values.toSeq.map {
    t =>
      val benefits =
        if (t >= Tier.Friend) {
          val configKey = s"benefits.${t.toString.toLowerCase}."
          Benefits(
            config.getString(configKey + "leadin"),
            config.getStringList(configKey + "list").toSeq,
            config.getString(configKey + "priceText"),
            config.getString(configKey + "cta")
          )
        } else {
          Benefits("", Seq.empty, "", "")
        }
      t -> benefits
  }.toMap

}
