package com.gu.memsub
import com.gu.zuora.rest

object Subscription {
  case class Name(get: String) extends AnyVal
  case class Id(get: String) extends AnyVal
  case class AccountId(get: String) extends AnyVal
  case class AccountNumber(get: String) extends AnyVal
  case class ProductRatePlanId(get: String) extends AnyVal
  case class RatePlanId(get: String) extends AnyVal
  case class ProductId(get: String) extends AnyVal
  case class ProductRatePlanChargeId(get: String) extends AnyVal
  case class SubscriptionRatePlanChargeId(get: String) extends AnyVal

  case class Feature(id: Feature.Id, code: Feature.Code)

  object Feature {
    case class Id(get: String) extends AnyVal
    case class Code(get: String) extends AnyVal

    object Code {
      val Events = Code("Events")
      val Books = Code("Books")
    }

    def fromRest(f: rest.Feature) =
      Feature(Id(f.id), Code(f.featureCode))
  }
}

