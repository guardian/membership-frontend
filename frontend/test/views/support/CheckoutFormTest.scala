package views.support

import com.gu.i18n._
import com.gu.identity.play.{PrivateFields, StatusFields}
import com.gu.membership.{PaidMembershipPlan, PaidMembershipPlans}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub._
import com.gu.salesforce.Tier
import com.gu.salesforce.Tier.Partner
import org.specs2.mutable.Specification
import TierPlans._

class CheckoutFormTest extends Specification {
  val billingAddress = PrivateFields(
    billingAddress1 = Some("billingAddress1"),
    billingAddress2 = Some("billingAddress2"),
    billingAddress3 = Some("billingAddress3"),
    billingAddress4 = Some("billingAddress4"),
    billingCountry = CountryGroup.countryByCode("US").map(_.name))

  val idUser =
    IdentityUser(
      billingAddress,
      StatusFields(),
      passwordExists = false)

  val idUserWithBlankCountry =
    IdentityUser(
      billingAddress.copy(billingCountry = None),
      StatusFields(),
      passwordExists = false)

  val pricingSummary = PricingSummary(Map(
    GBP -> Price(1.5f, GBP),
    USD -> Price(2.5f, USD),
    EUR -> Price(3.5f, EUR)
  ))

  val plans = PaidMembershipPlans[Current, Partner](
    month = PaidMembershipPlan[Current, Partner, Month](Status.current, Tier.partner, BillingPeriod.month, ProductRatePlanId("month"), pricingSummary),
    year = PaidMembershipPlan[Current, Partner, Year](Status.current, Tier.partner, BillingPeriod.year, ProductRatePlanId("year"), pricingSummary)
  )

  implicit class checkoutForm2Tuple2(form: CheckoutForm) {
    def tupled2 = (form.defaultCountry, form.currency)
  }

  "CheckoutFormTest" should {
    "forIdentityUser" should {
      "when a country group is set from the request" should {
        "set country/currency from the identity user" in {
          CheckoutForm.forIdentityUser(
            idUser, plans, Some(CountryGroup.Australia)
          ).tupled2 === (Some(Country.US), USD)
        }

        "and the country group currency is available" should {
          "use the country group for both country and currency" in {
            CheckoutForm.forIdentityUser(
              idUserWithBlankCountry, plans, Some(CountryGroup.Europe)
            ).tupled2 === (None, EUR)
          }
        }

        "and the country group currency is not available" should {
          "fallback to the request country group and set currency to GBP" in {
            CheckoutForm.forIdentityUser(
              idUserWithBlankCountry, plans, Some(CountryGroup.Australia)
            ).tupled2 === (Some(Country.Australia), GBP)
          }
        }
      }

      "when no country group is set" should {
        "set country/currency from the identity user" in {
          CheckoutForm.forIdentityUser(
            idUser, plans, None
          ).tupled2 === (Some(Country.US), USD)
        }

        "fallback to the UK country group if the identity user has no address info" in {
          CheckoutForm.forIdentityUser(
            idUserWithBlankCountry, plans, None
          ).tupled2 === (Some(Country.UK), GBP)
        }
      }
    }

  }
}
