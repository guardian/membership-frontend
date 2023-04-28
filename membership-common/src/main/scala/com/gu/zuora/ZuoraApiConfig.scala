package com.gu.zuora

import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.dsl._

object ZuoraApiConfig {

  private def username(c: com.typesafe.config.Config) = c.getString("zuora.api.username")
  private def password(c: com.typesafe.config.Config) = c.getString("zuora.api.password")

  def soap(c: com.typesafe.config.Config, environmentName: String) =
    ZuoraSoapConfig(environmentName, c getString "zuora.api.url", username(c), password(c))

  def rest(c: com.typesafe.config.Config, environmentName: String) =
    ZuoraRestConfig(environmentName, c getString "zuora.api.restUrl", username(c), password(c))
}


case class ZuoraRestConfig(envName: String, url: Uri, username: String, password: String)
case class ZuoraSoapConfig(envName: String, url: Uri, username: String, password: String)

