package com.gu.memsub.subsv2

import com.gu.config.SubsV2ProductIds
import com.typesafe.config.ConfigFactory

object Fixtures {
  lazy val uat = ConfigFactory.parseResources("touchpoint.UAT.conf")
  lazy val productIds = SubsV2ProductIds(uat.getConfig("touchpoint.backend.environments.UAT.zuora.productIds"))
}
