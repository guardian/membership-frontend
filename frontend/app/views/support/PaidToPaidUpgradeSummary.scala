package views.support

import com.gu.membership.MembershipCatalog
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub._
import com.gu.salesforce.PaidTier
import com.gu.services.model.BillingSchedule
import model.PaidSubscription
import model.SubscriptionOps._
import org.joda.time.{DateTime, LocalDate}

case class CurrentSummary(
    tier: PaidTier, startDate: LocalDate, payment: Price, card: PaymentCard)

case class TargetSummary(tier: PaidTier,
                         firstPayment: Price,
                         nextPayment: Price,
                         nextPaymentDate: LocalDate)

case class PaidToPaidUpgradeSummary(billingPeriod: BillingPeriod,
                                    current: CurrentSummary,
                                    target: TargetSummary) {
  lazy val transactionDate: LocalDate = DateTime.now.toLocalDate
}

object PaidToPaidUpgradeSummary {
  case class UpgradeSummaryError(
      subNumber: Subscription.Name, targetTier: PaidTier)(msg: String)
      extends Throwable {
    override def getMessage =
      s"Failure while trying to display an upgrade summary for the subscription $subNumber to $targetTier: $msg"
  }

  def apply(invoices: BillingSchedule,
            sub: PaidSubscription,
            targetId: ProductRatePlanId,
            card: PaymentCard)(
      implicit catalog: MembershipCatalog): PaidToPaidUpgradeSummary = {
    val plan = catalog.unsafeFindPaid(targetId)
    lazy val upgradeError = UpgradeSummaryError(sub.name, plan.tier) _
    val accountCurrency = sub.currency
    val firstPayment = Price(invoices.first.amount, accountCurrency)

    val billingPeriod = sub.plan.billingPeriod

    val targetPrice = plan.pricing
      .getPrice(accountCurrency)
      .getOrElse(
          throw upgradeError(
              s"Could not find a price for currency $accountCurrency for rate plan ${plan.productRatePlanId.get}")
      )

    val currentSummary = CurrentSummary(
        tier = sub.paidPlan.tier,
        startDate = sub.startDate,
        payment = sub.recurringPrice,
        card = card
    )

    val targetSummary = TargetSummary(
        tier = plan.tier,
        firstPayment = firstPayment,
        nextPayment = targetPrice,
        nextPaymentDate = billingPeriod match {
            case Year() => LocalDate.now().plusYears(1)
            case Month() => LocalDate.now().plusMonths(1)
            case _ =>
              throw new IllegalStateException(
                  s"Unreachable code: the plan ${plan.productRatePlanId} was expected to be either yearly or monthly")
          }
    )

    PaidToPaidUpgradeSummary(billingPeriod, currentSummary, targetSummary)
  }
}
