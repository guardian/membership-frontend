package configuration

import com.netaporter.uri.dsl._

import com.typesafe.config.ConfigFactory
import com.gu.identity.cookie.{ PreProductionKeys, ProductionKeys }
import play.api.mvc.Request

object Config {
  val config = ConfigFactory.load()

  lazy val awsAccessKey = config.getString("aws.access.key")
  lazy val awsSecretKey = config.getString("aws.secret.key")

  val membershipUrl = config.getString("membership.url")
  val membershipHide = config.getBoolean("membership.hide")

  val idWebAppUrl = config.getString("identity.webapp.url")
  def idWebAppSigninUrl(uri: String): String =
    (idWebAppUrl / "signin") ? ("returnUrl" -> s"$membershipUrl$uri")
  val idKeys = if (config.getBoolean("identity.production.keys")) new ProductionKeys else new PreProductionKeys

  val eventbriteApiUrl = config.getString("eventbrite.api.url")
  val eventbriteApiToken = config.getString("eventbrite.api.token")
  val eventbriteApiEventListUrl = config.getString("eventbrite.api.event-list-url")

  val stripeApiURL = config.getString("stripe.api.url")
  val stripeApiSecret = config.getString("stripe.api.secret")

  val googleAnalyticsTrackingId = config.getString("google.analytics.tracking.id")

  val corsAllowOrigin = config.getString("cors.allow.origin")
}
