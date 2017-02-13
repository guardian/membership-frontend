package views.support

import com.gu.i18n.Currency
import com.gu.memsub.BillingPeriod.{Month, Year}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.{Subscription => S, _}
import com.gu.memsub.subsv2._
import com.gu.salesforce.PaidTier
import com.gu.memsub.BillingSchedule
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import views.support.MembershipCompat._

case class CurrentSummary(tier: PaidTier, startDate: LocalDate, payment: Price, card: PaymentCard)

case class TargetSummary(tier: PaidTier, firstPayment: Price, nextPayment: Price, nextPaymentDate: LocalDate)

case class PaidToPaidUpgradeSummary(billingPeriod: BillingPeriod, current: CurrentSummary, target: TargetSummary) {
  lazy val transactionDate: LocalDate = DateTime.now.toLocalDate
}

object PaidToPaidUpgradeSummary {
  case class UpgradeSummaryError(subNumber: S.Name, targetTier: PaidTier)(msg: String) extends Throwable {
    override def getMessage = s"Failure while trying to display an upgrade summary for the subscription $subNumber to $targetTier: $msg"
  }

  def apply(invoices: BillingSchedule, sub: Subscription[SubscriptionPlan.PaidMember], targetId: ProductRatePlanId, card: PaymentCard)(implicit catalog: Catalog): PaidToPaidUpgradeSummary = {
    val plan = catalog.unsafeFindPaid(targetId)
    lazy val upgradeError = UpgradeSummaryError(sub.name, plan.tier) _
    val accountCurrency = sub.plan.currency
    val firstPayment = Price(invoices.first.amount, accountCurrency)


    val billingPeriod = sub.plan.charges.billingPeriod

    val targetPrice = plan.charges.price.getPrice(accountCurrency).getOrElse(
      throw upgradeError(s"Could not find a price for currency $accountCurrency for rate plan ${plan.id.get}")
    )

    val currentSummary =
      CurrentSummary(
        tier = sub.plan.tier,
        startDate = sub.startDate,
        payment = sub.plan.charges.price.prices.head,
        card = card
      )

    val targetSummary =
      TargetSummary(
        tier = plan.tier,
        firstPayment =  firstPayment,
        nextPayment = targetPrice,
        nextPaymentDate = billingPeriod match {
          case Year => LocalDate.now().plusYears(1)
          case Month => LocalDate.now().plusMonths(1)
          case _ => throw new IllegalStateException(s"Unreachable code: the plan ${plan.id} was expected to be either yearly or monthly")
        }
      )

    PaidToPaidUpgradeSummary(billingPeriod, currentSummary, targetSummary)
  }


  implicit val currencyWrites = new Writes[Currency] {
    override def writes(o: Currency): JsValue = JsString(o.glyph)
  }

  implicit val tierWrites = new Writes[PaidTier] {
    override def writes(o: PaidTier): JsValue = JsString(o.name)
  }

  implicit val paymentCardWrites = Json.writes[PaymentCard]
  implicit val priceWrites = Json.writes[Price]

  implicit val billingPeriodWrites = new Writes[BillingPeriod] {
    override def writes(o: BillingPeriod): JsValue = JsString(o.noun)
  }

  implicit val currentSummaryWrites = Json.writes[CurrentSummary]
  implicit val targetSummaryWrites = Json.writes[TargetSummary]

  implicit val summaryWrites: Writes[PaidToPaidUpgradeSummary] = (
    (JsPath \ "billingPeriod").write[BillingPeriod] and
      (JsPath \ "currentSummary").write[CurrentSummary] and
      (JsPath \ "targetSummary").write[TargetSummary]
    )(unlift(PaidToPaidUpgradeSummary.unapply))
}
