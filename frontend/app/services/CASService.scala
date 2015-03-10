package services

import com.gu.cas.CASService
import configuration.Config
import monitoring.CASMetrics


object CASService extends CASService(Config.casServiceConfig, CASMetrics)
