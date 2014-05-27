package configuration

import com.typesafe.config.ConfigFactory
import com.gu.identity.cookie.{ PreProductionKeys, ProductionKeys }

object Config {
  val config = ConfigFactory.load()

  val membershipUrl = config.getString("membership.url")
  val membershipHide = config.getBoolean("membership.hide")

  val idWebAppUrl = config.getString("identity.webapp.url")
  val idKeys = if (config.getBoolean("identity.production.keys")) new ProductionKeys else new PreProductionKeys

  val eventListUrl: String = config.getString("eventbrite.event-list-url")
  val eventUrl: String = config.getString("eventbrite.event-url")
  val eventToken: (String, String) = ("token", config.getString("eventbrite.token"))

  val stripeApiURL = config.getString("stripe.api.url")
  val stripeApiSecret = config.getString("stripe.api.secret")
}
