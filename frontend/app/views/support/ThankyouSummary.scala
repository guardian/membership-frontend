package views.support

import com.gu.membership.model.{BillingPeriod, Price}
import org.joda.time.DateTime
import views.support.ThankyouSummary.NextPayment

case class ThankyouSummary(startDate: DateTime,
                           amountPaidToday: Price,
                           planAmount: Price,
                           nextPayment: Option[NextPayment],
                           renewalDate: Option[DateTime],
                           initialFreePeriodOffer: Boolean,
                           billingPeriod: BillingPeriod)

object ThankyouSummary {
  case class NextPayment(price: Price, date: DateTime)
}
