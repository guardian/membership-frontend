package com.gu.salesforce

import com.typesafe.config.Config

case class SalesforceConfig(
  envName: String,
  url: String,
  key: String,
  secret: String,
  username: String,
  password: String,
  token: String,
  recordTypeIds: Config
)

object SalesforceConfig {
  def from(config: Config, environmentName: String) = SalesforceConfig(
    environmentName,
    url = config.getString("salesforce.url"),
    key = config.getString("salesforce.consumer.key"),
    secret = config.getString("salesforce.consumer.secret"),
    username = config.getString("salesforce.api.username"),
    password = config.getString("salesforce.api.password"),
    token = config.getString("salesforce.api.token"),
    recordTypeIds = config.getConfig("salesforce.record-type-ids")
  )
}
