package model

import org.joda.time.DateTime

case class MembershipSummary(startDate: DateTime,
                             endDateForFirstPayment: DateTime,
                             amountPaidToday: Float,
                             planAmount: Float,
                             nextPaymentPrice: Option[Float],
                             nextPaymentDate: DateTime) {

  val annual = endDateForFirstPayment.plusDays(1) == startDate.plusYears(1)

}
