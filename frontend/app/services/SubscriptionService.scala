package services

import configuration.Config

trait SubscriptionService {
  val apiUsername: String
  val apiPassword: String
}

object SubscriptionService extends SubscriptionService {
  val apiUsername = Config.zuoraApiUsername
  val apiPassword = Config.zuoraApiPassword
}
