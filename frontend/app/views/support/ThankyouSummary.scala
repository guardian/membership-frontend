package views.support

import com.gu.memsub.{BillingPeriod, Price}
import org.joda.time.LocalDate
import views.support.ThankyouSummary.NextPayment

case class ThankyouSummary(startDate: LocalDate,
                           amountPaidToday: Price,
                           planAmount: Price,
                           nextPayment: Option[NextPayment],
                           renewalDate: Option[LocalDate],
                           initialFreePeriodOffer: Boolean,
                           billingPeriod: BillingPeriod)

object ThankyouSummary {
  case class NextPayment(price: Price, date: LocalDate)
}
