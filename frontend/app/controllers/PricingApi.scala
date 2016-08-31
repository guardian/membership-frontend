package controllers

import com.gu.i18n._
import com.gu.memsub.subsv2.Catalog
import com.gu.salesforce.PaidTier
import play.api.libs.json.{JsArray, JsString, JsValue, Json, Writes}
import play.api.mvc.Controller
import services.TouchpointBackend
import views.support.{CountryWithCurrency, Pricing}
import views.support.Pricing._
import views.support.MembershipCompat._

case class MembershipPlan(tier: PaidTier, prices: List[Pricing])

case class MembershipPlanResponse(plans: List[MembershipPlan])

object PricingFormats {
  implicit val currencyWrites = new Writes[Currency] {
    override def writes(currency: Currency): JsValue = currency match {
      case GBP => JsString("GBP")
      case AUD => JsString("AUD")
      case EUR => JsString("EUR")
      case NZD => JsString("NZD")
      case CAD => JsString("CAD")
      case USD => JsString("USD")
    }
  }
  implicit val countryWrites = Json.writes[Country]
  implicit val countryCurrencyWrites = Json.writes[CountryWithCurrency]

  implicit val pricingsWrites = new Writes[List[Pricing]] {
    override def writes(pricings: List[Pricing]): JsValue = {
      JsArray(
        pricings
          .flatMap { pricing => Seq("monthly" -> pricing.monthly, "annually" -> pricing.yearly) }
          .map { case (bp, price) =>
            Json.obj(
              "billingPeriod" -> bp,
              "amount" -> f"${ price.amount }%.2f",
              "currency" -> price.currency
            )
          }
      )
    }
  }

  implicit val tierWrites = new Writes[PaidTier] {
    override def writes(tier: PaidTier): JsValue = JsString(tier.name)
  }
  implicit val planWrites = Json.writes[MembershipPlan]
  implicit val writes = Json.writes[MembershipPlanResponse]
}

object PricingApi extends Controller {

  import PricingFormats._
  import views.support.Pricing._

  val membersCatalog: Catalog = TouchpointBackend.Normal.catalog

  def currencies = CachedAction {
    Ok(Json.toJson(CountryWithCurrency.all))
  }

  def ratePlans = CachedAction {
    val plansByTier = for {
      tier <- PaidTier.all
      plan = membersCatalog.findPaid(tier)
    } yield MembershipPlan(plan.tier, plan.allPricing)

    Ok(Json.toJson(MembershipPlanResponse(plansByTier.toList)))
  }
}

