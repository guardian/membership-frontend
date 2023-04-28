package com.gu.touchpoint

import com.gu.salesforce.SalesforceConfig
import com.gu.zuora.{ZuoraApiConfig, ZuoraRestConfig, ZuoraSoapConfig}
import com.gu.monitoring.SafeLogger

case class TouchpointBackendConfig(
  environmentName: String,
  salesforce: SalesforceConfig,
  zuoraSoap: ZuoraSoapConfig,
  zuoraRest: ZuoraRestConfig,
)

object TouchpointBackendConfig {

  sealed abstract class BackendType(val name: String)
  object BackendType {
    object Default extends BackendType("default")
    object Testing extends BackendType("test")
  }

  def byType(typ: BackendType = BackendType.Default, config: com.typesafe.config.Config) = {
    val backendsConfig = config.getConfig("touchpoint.backend")
    val environmentName = backendsConfig.getString(typ.name)

    val touchpointBackendConfig = byEnv(environmentName, backendsConfig)

    SafeLogger.info(s"TouchPoint config - $typ: config=${touchpointBackendConfig.hashCode}")

    touchpointBackendConfig
  }

  def byEnv(environmentName: String, backendsConfig: com.typesafe.config.Config) = {
    val envBackendConf = backendsConfig.getConfig(s"environments.$environmentName")

    TouchpointBackendConfig(
      environmentName,
      SalesforceConfig.from(envBackendConf, environmentName),
      ZuoraApiConfig.soap(envBackendConf, environmentName),
      ZuoraApiConfig.rest(envBackendConf, environmentName),
    )
  }
}
