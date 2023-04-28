package com.gu.config

import com.gu.memsub.ProductFamily
import com.gu.memsub.Subscription.ProductRatePlanId
import com.typesafe.config.{ConfigFactory, Config}

trait ProductFamilyRatePlanIds {
  def productRatePlanIds: Set[ProductRatePlanId]
}

object ProductFamilyRatePlanIds {
  def config(context: Option[Config] = None)(env: String, productFamily: ProductFamily): Config =
    context.getOrElse(ConfigFactory.load).getConfig(s"touchpoint.backend.environments.$env.zuora.ratePlanIds.${productFamily.id}")
}
