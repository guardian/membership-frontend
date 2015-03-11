package services

import com.gu.cas._
import configuration.Config
import monitoring.CASMetrics

object CASApi extends CASApi(Config.casServiceConfig, CASMetrics)
object CASService extends CASService(CASApi)
