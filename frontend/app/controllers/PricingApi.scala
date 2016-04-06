package controllers

import com.gu.i18n._
import play.api.libs.json.{JsString, JsValue, Writes, Json}
import play.api.mvc.Controller
import views.support.CountryWithCurrency

object CountryWithCurrencyResponse {
  implicit val countryWrites = Json.writes[Country]
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
  implicit val countryCurrencyWrites = Json.writes[CountryWithCurrency]
}

object PricingApi extends Controller {
  import CountryWithCurrencyResponse._
  def currencies = CachedAction {
    Ok(Json.toJson(CountryWithCurrency.all))
  }
}
