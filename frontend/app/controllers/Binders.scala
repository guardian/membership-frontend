package controllers

import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.BillingPeriod
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PromoCode
import com.gu.salesforce.{PaidTier, Tier, FreeTier}
import play.api.mvc.PathBindable.{Parsing => PathParsing}
import play.api.mvc.QueryStringBindable.{Parsing => QueryParsing}
import scala.reflect.runtime.universe._

object Binders {
  def applyNonEmpty[A : TypeTag](f: String => A)(s: String): A =
    if (s.isEmpty) {
      val msg =
        s"Cannot build a ${implicitly[TypeTag[A]].tpe} from an empty string"
      throw new IllegalArgumentException(msg)
    } else f(s)

  implicit object bindableTier
      extends PathParsing[Tier](
          Tier.slugMap,
          _.slug,
          (key: String, e: Exception) =>
            s"Cannot parse parameter $key as a Tier: ${e.getMessage}"
      )

  implicit object bindableTierQuery
      extends QueryParsing[Tier](
          Tier.slugMap,
          _.slug,
          (key: String, e: Exception) =>
            s"Cannot parse parameter $key as a Tier: ${e.getMessage}"
      )

  implicit object bindablePaidTier
      extends PathParsing[PaidTier](
          PaidTier.slugMap,
          _.slug,
          (key: String, e: Exception) =>
            s"Cannot parse parameter $key as a PaidTier: ${e.getMessage}"
      )

  implicit object bindableFreeTier
      extends PathParsing[FreeTier](
          FreeTier.slugMap,
          _.slug,
          (key: String, e: Exception) =>
            s"Cannot parse parameter $key as a FreeTier: ${e.getMessage}"
      )

  implicit object bindableCountryGroupPathParser
      extends PathParsing[CountryGroup](
          id => CountryGroup.byId(id).get,
          _.id,
          (key: String, _: Exception) =>
            s"Cannot parse path parameter $key as a CountryGroup"
      )

  implicit object bindableCountryGroupQueryParser
      extends QueryParsing[CountryGroup](
          id => CountryGroup.byId(id).get,
          _.id,
          (key: String,
          _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
      )

  implicit object bindablePrpId
      extends QueryParsing[ProductRatePlanId](
          applyNonEmpty(ProductRatePlanId),
          _.get,
          (key: String,
          _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
      )

  implicit object bindablePromoCode
      extends QueryParsing[PromoCode](
          applyNonEmpty(PromoCode),
          _.get,
          (key: String,
          _: Exception) => s"Cannot parse parameter $key as a PromoCode"
      )

  implicit object bindableCountry
      extends QueryParsing[Country](
          alpha2 => CountryGroup.countryByCode(alpha2).get,
          _.alpha2,
          (key: String,
          _: Exception) => s"Cannot parse parameter $key as a Country"
      )

  implicit object bindableBillingPeriod
      extends QueryParsing[BillingPeriod](
          adjective => Seq(month, year).find(_.adjective == adjective).get,
          _.adjective,
          (key: String,
          _: Exception) => s"Cannot parse parameter $key as a Billing Period"
      )
}
