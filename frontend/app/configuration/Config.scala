package configuration

import com.typesafe.config.ConfigFactory

object Config {
  val config = ConfigFactory.load()

  val awsAccessKey = config.getString("aws.access.key")
  val awsSecretKey = config.getString("aws.secret.key")

  val membershipUrl = config.getString("membership.url")
  val membershipHide = config.getBoolean("membership.hide")

  val idWebAppUrl = config.getString("identity.webapp.url")

  val eventListUrl: String = config.getString("eventbrite.event-list-url")
  val eventUrl: String = config.getString("eventbrite.event-url")
  val eventToken: (String, String) = ("token", config.getString("eventbrite.token"))

  val stripeApiURL = config.getString("stripe.api.url")
  val stripeApiSecret = config.getString("stripe.api.secret")
}
