package views.support

import com.gu.memsub.BillingPeriod.{Month, Year}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.subsv2._
import com.gu.memsub.{BillingSchedule, Subscription => S, _}
import com.gu.salesforce.PaidTier
import org.joda.time.{DateTime, LocalDate}
import views.support.MembershipCompat._

case class CurrentSummary(tier: PaidTier, startDate: LocalDate, payment: Price, paymentMethod: PaymentMethod)

case class TargetSummary(tier: PaidTier, firstPayment: Price, nextPayment: Price, nextPaymentDate: LocalDate)

case class PaidToPaidUpgradeSummary(billingPeriod: BillingPeriod, current: CurrentSummary, target: TargetSummary) {
  lazy val transactionDate: LocalDate = DateTime.now.toLocalDate
}

object PaidToPaidUpgradeSummary {
  case class UpgradeSummaryError(subNumber: S.Name, targetTier: PaidTier)(msg: String) extends Throwable {
    override def getMessage = s"Failure while trying to display an upgrade summary for the subscription $subNumber to $targetTier: $msg"
  }

  def apply(invoices: BillingSchedule, sub: Subscription[SubscriptionPlan.PaidMember], targetId: ProductRatePlanId, paymentMethod: PaymentMethod)(implicit catalog: Catalog): PaidToPaidUpgradeSummary = {
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
        paymentMethod = paymentMethod
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
}
