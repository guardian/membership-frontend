package views.support

import com.gu.i18n.Currency.GBP
import com.gu.i18n._
import com.gu.memsub.BillingPeriod
import com.typesafe.scalalogging.LazyLogging

case class CheckoutForm(defaultCountry: Option[Country],
                        currency: Currency,
                        billingPeriod: BillingPeriod)

object CheckoutForm extends LazyLogging {
  def forIdentityUser(userCountry: Option[Country], plans: TierPlans, requestCountryGroup: Option[CountryGroup]): CheckoutForm = {
    val (country, countryGroup) = (requestCountryGroup, userCountry) match {
      case (Some(cg), _) =>
        (cg.defaultCountry, cg)
      case (_, Some(idCountry)) =>
        (Some(idCountry), countryGroupOf(idCountry))
      case _ =>
        (Some(Country.UK), CountryGroup.UK)
    }

    val desiredCurrency = countryGroup.currency

    val currency =
      if (plans.currencies.contains(desiredCurrency)) desiredCurrency else GBP

    CheckoutForm(country, currency, BillingPeriod.Year)
  }

   def countryGroupOf(country: Country): CountryGroup = CountryGroup.allGroups
    .find(_.countries.contains(country))
    .getOrElse {
      logger.warn(s"Could not find country $country in any CountryGroup, defaulting to UK")
      CountryGroup.UK
    }
}
