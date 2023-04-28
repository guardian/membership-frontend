package com.gu.config

import com.gu.memsub.Subscription.{ProductRatePlanChargeId, ProductRatePlanId}

case class DiscountRatePlan(planId: ProductRatePlanId, planChargeId: ProductRatePlanChargeId)
case class DiscountRatePlanIds(percentageDiscount: DiscountRatePlan)

