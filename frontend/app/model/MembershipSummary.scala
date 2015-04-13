package model

import org.joda.time.DateTime

case class MembershipSummary(startDate: DateTime,
                             firstPaymentEndDate: DateTime,
                             amountPaidToday: Float,
                             planAmount: Float,
                             nextPaymentPrice: Float,
                             nextPaymentDate: DateTime,
                             renewalDate: DateTime,
                             initialFreePeriodOffer: Boolean) {


  val annual = startDate.plusYears(1) == renewalDate

}