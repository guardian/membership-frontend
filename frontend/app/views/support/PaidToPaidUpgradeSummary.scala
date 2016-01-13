package views.support

import com.gu.membership.MembershipCatalog
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub._
import com.gu.salesforce.PaidTier
import com.gu.stripe.Stripe.Card
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import model.PaidSubscription
import model.SubscriptionOps._
import org.joda.time.{DateTime, LocalDate}

case class CurrentSummary(tier: PaidTier, startDate: LocalDate, payment: Price, card: Card)

case class TargetSummary(tier: PaidTier, firstPayment: Price, nextPayment: Price, nextPaymentDate: LocalDate)

case class PaidToPaidUpgradeSummary(billingPeriod: BillingPeriod, current: CurrentSummary, target: TargetSummary) {
  lazy val transactionDate: LocalDate = DateTime.now.toLocalDate
}

object PaidToPaidUpgradeSummary {
  case class UpgradeSummaryError(subNumber: Subscription.Name, targetTier: PaidTier)(msg: String) extends Throwable {
    override def getMessage = s"Failure while trying to display an upgrade summary for the subscription $subNumber to $targetTier: $msg"
  }

  def apply(invoices: Seq[PreviewInvoiceItem], sub: PaidSubscription, targetId: ProductRatePlanId, card: Card)(implicit catalog: MembershipCatalog): PaidToPaidUpgradeSummary = {
    val plan = catalog.unsafeFindPaid(targetId)
    lazy val upgradeError = UpgradeSummaryError(sub.name, plan.tier) _
    val accountCurrency = sub.currency

    // The sorted invoice items list will include as its first element a refund with a negative
    // price. We add up the refund to the next invoice item, thus computing a pro-rated price for the upgrade.
    val firstPayment = invoices.sortBy(_.price) match {
      case refundItem :: targetPlanItem :: _ if refundItem.price < 0 =>
        Price(refundItem.price + targetPlanItem.price, accountCurrency)
      case _ => throw new IllegalStateException(
        s"Failed to compute a pro-rated price from invoice items $invoices. Subscription: ${sub.name.get}, target tier: ${plan.tier.name}")
    }

    val billingPeriod = sub.plan.billingPeriod

    val targetPrice = plan.pricing.getPrice(accountCurrency).getOrElse(
      throw upgradeError(s"Could not find a price for currency $accountCurrency for rate plan ${plan.productRatePlanId.get}")
    )

    val currentSummary =
      CurrentSummary(
        tier = sub.paidPlan.tier,
        startDate = sub.startDate,
        payment = sub.recurringPrice,
        card = card
      )

    val targetSummary =
      TargetSummary(
        tier = plan.tier,
        firstPayment =  firstPayment,
        nextPayment = targetPrice,
        nextPaymentDate = billingPeriod match {
          case Year() => LocalDate.now().plusYears(1)
          case Month() => LocalDate.now().plusMonths(1)
          case _ => throw new IllegalStateException(s"Unreachable code: the plan ${plan.productRatePlanId} was expected to be either yearly or monthly")
        }
      )

    PaidToPaidUpgradeSummary(billingPeriod, currentSummary, targetSummary)
  }
}
