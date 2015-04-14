package model

import org.joda.time.DateTime

case class MembershipSummary(startDate: DateTime,
                             firstPaymentEndDate: DateTime,
                             amountPaidToday: Option[Float],
                             planAmount: Float,
                             nextPaymentPrice: Float,
                             nextPaymentDate: DateTime,
                             renewalDate: DateTime) {

  val initialFreePeriodOffer = amountPaidToday.isEmpty

  val annual = startDate.plusYears(1) == renewalDate

}