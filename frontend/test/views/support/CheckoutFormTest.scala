package views.support

import com.gu.i18n.Currency._
import com.gu.i18n._
import com.gu.identity.play.{PrivateFields, StatusFields}
import com.gu.memsub.Benefit._
import com.gu.memsub.Subscription.{ProductRatePlanChargeId, ProductRatePlanId}
import com.gu.memsub._
import com.gu.memsub.subsv2.{CatalogPlan, PaidCharge, PaidMembershipPlans}
import org.specs2.mutable.Specification


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
      passwordExists = false,
      email = "test@gu.com"
    )

  val idUserWithBlankCountry =
    IdentityUser(
      billingAddress.copy(billingCountry = None),
      StatusFields(),
      passwordExists = false,
      email = "test@gu.com"
    )

  val pricingSummary = PricingSummary(Map(
    GBP -> Price(1.5f, GBP),
    USD -> Price(2.5f, USD),
    EUR -> Price(3.5f, EUR)
  ))

  val plans = PaidMembershipPlans[Partner.type](
    month = CatalogPlan(ProductRatePlanId(""), Product.Membership, "Partner", "Partner", None, PaidCharge(Partner, BillingPeriod.Month, pricingSummary, ProductRatePlanChargeId("prpcId")), Status.current),
    year = CatalogPlan(ProductRatePlanId(""), Product.Membership, "Partner", "Partner", None, PaidCharge(Partner, BillingPeriod.Year, pricingSummary, ProductRatePlanChargeId("prpcId")), Status.current)
  )

  implicit class checkoutForm2Tuple2(form: CheckoutForm) {
    def tupled2 = (form.defaultCountry, form.currency)
  }

  "CheckoutFormTest" should {
    "forIdentityUser" should {
      "when a country group is set from the request" should {
        "and the country group currency is available" should {
          "set country/currency from the request" in {
            CheckoutForm.forIdentityUser(
              None, PaidPlans(plans), Some(CountryGroup.Europe)
            ).tupled2 ===(None, EUR)
          }
        }

        "and the country group currency is not available" should {
          "fallback to the request country group and set currency to GBP" in {
            CheckoutForm.forIdentityUser(
              None, PaidPlans(plans), Some(CountryGroup.Australia)
            ).tupled2 === (Some(Country.Australia), GBP)
          }
        }
      }

      "when no country group is set" should {
        "set country/currency from the identity user" in {
          CheckoutForm.forIdentityUser(
            Some(Country.US), PaidPlans(plans), None
          ).tupled2 === (Some(Country.US), USD)
        }

        "fallback to the UK country group if the identity user has no address info" in {
          CheckoutForm.forIdentityUser(
            None, PaidPlans(plans), None
          ).tupled2 === (Some(Country.UK), GBP)
        }
      }
    }

  }
}
