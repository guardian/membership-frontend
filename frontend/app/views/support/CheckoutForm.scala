package views.support

import com.gu.i18n.Currency.GBP
import com.gu.i18n._
import com.gu.memsub.BillingPeriod
import com.typesafe.scalalogging.LazyLogging

case class CheckoutForm(defaultCountry: Option[Country],
                        currency: Currency,
                        billingPeriod: BillingPeriod)

object CheckoutForm extends LazyLogging {
  def forIdentityUser(idUser: IdentityUser, plans: TierPlans, requestCountryGroup: Option[CountryGroup]): CheckoutForm = {
    val (country, desiredCurrency) = (requestCountryGroup, idUser.country) match {
      case (Some(cg), _) =>
        (cg.defaultCountry, cg.currency)
      case (_, Some(idCountry)) =>
        val currency = CountryGroup.allGroups
                         .find(_.countries.contains(idCountry))
                         .getOrElse {
                           logger.warn(s"Could not find country $idCountry in any CountryGroup, defaulting to UK")
                           CountryGroup.UK
                         }.currency
        (Some(idCountry), currency)
      case _ =>
        (Some(Country.UK), CountryGroup.UK.currency)
    }

    val currency =
      if (plans.currencies.contains(desiredCurrency)) desiredCurrency else GBP

    CheckoutForm(country, currency, BillingPeriod.Year)
  }
}
