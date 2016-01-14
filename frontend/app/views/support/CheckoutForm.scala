package views.support

import com.gu.i18n._
import com.gu.memsub.BillingPeriod

case class CheckoutForm(defaultCountry: Option[Country],
                        currency: Currency,
                        billingPeriod: BillingPeriod)

object CheckoutForm {
  def forIdentityUser(idUser: IdentityUser, plans: TierPlans, requestCountryGroup: Option[CountryGroup]): CheckoutForm = {
    val countryGroup = idUser.countryGroup.orElse(requestCountryGroup).getOrElse(CountryGroup.UK)
    val country = idUser.country.orElse(countryGroup.defaultCountry)

    val currency =
      //Get the currency by country
      country.flatMap(plans.currency)
        //alternatively, use the countryGroup currency if available in the selected plan
        .orElse(Some(countryGroup.currency)).filter(plans.currencies)
        //or fallback to GBP
        .getOrElse(GBP)

    CheckoutForm(country, currency, BillingPeriod.year)
  }
}
