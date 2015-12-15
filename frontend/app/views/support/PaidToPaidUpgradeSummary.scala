package views.support

import com.github.nscala_time.time.OrderingImplicits.DateTimeOrdering
import com.gu.membership.model._
import com.gu.salesforce.PaidTier
import com.gu.stripe.Stripe.Card
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import model.MembershipCatalog
import org.joda.time.{DateTime, LocalDate}

case class CurrentSummary(tier: PaidTier, startDate: LocalDate, payment: Price, card: Card)

case class TargetSummary(tier: PaidTier, firstPayment: Price, nextPayment: Price, nextPaymentDate: LocalDate)

case class PaidToPaidUpgradeSummary(billingPeriod: BillingPeriod, current: CurrentSummary, target: TargetSummary) {
  lazy val transactionDate: LocalDate = DateTime.now.toLocalDate
}

object PaidToPaidUpgradeSummary {
  case class UpgradeSummaryError(subNumber: String, targetTier: PaidTier)(msg: String) extends Throwable {
    override def getMessage = s"Failure while trying to display an upgrade summary for the subscription $subNumber to $targetTier: $msg"
  }

  def apply(catalog: MembershipCatalog, invoices: Seq[PreviewInvoiceItem], sub: model.PaidSubscription, targetTier: PaidTier, card: Card): PaidToPaidUpgradeSummary = {
    val upgradeError = UpgradeSummaryError(sub.number, targetTier) _
    val accountCurrency = sub.accountCurrency

    // The sorted invoice items list will include as its first element a refund with a negative
    // price. We add up the refund to the next invoice item, thus computing a pro-rated price for the upgrade.
    val firstPayment = invoices.sortBy(_.price) match {
      case refundItem :: targetPlanItem :: _ if refundItem.price < 0 =>
        Price(refundItem.price + targetPlanItem.price, accountCurrency)
      case _ => throw new IllegalStateException(
        s"Failed to compute a pro-rated price from invoice items $invoices. Subscription: ${sub.number}, target tier: $targetTier")
    }

    val billingPeriod = sub.plan.billingPeriod
    val targetTierPlan = PaidTierPlan(targetTier, billingPeriod, Current)

    val targetTierPlanDetails = catalog.paidTierPlanDetails(targetTierPlan)
    val targetPrice = targetTierPlanDetails.pricingByCurrency.getPrice(accountCurrency).getOrElse(
      throw upgradeError(s"Could not find a price for currency $accountCurrency for rate plan $targetTierPlan")
    )

    val currentSummary =
      CurrentSummary(
        tier = sub.plan.tier,
        startDate = sub.startDate,
        payment = sub.recurringPrice,
        card = card
      )

    val targetSummary =
      TargetSummary(
        tier = targetTier,
        firstPayment =  firstPayment,
        nextPayment = targetPrice,
        nextPaymentDate = billingPeriod match {
          case Year => LocalDate.now().plusYears(1)
          case Month => LocalDate.now().plusMonths(1)
        }
      )

    PaidToPaidUpgradeSummary(billingPeriod, currentSummary, targetSummary)
  }
}
