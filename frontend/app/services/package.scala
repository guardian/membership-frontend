import configuration.Config
import services.zuora.ZuoraService

package object services {
  lazy val touchpointBackend = TouchpointBackend(Config.touchpointBackendConfig)
}
