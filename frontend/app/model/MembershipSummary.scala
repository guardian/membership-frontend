package model

import org.joda.time.DateTime

case class MembershipSummary(startDate: DateTime,
                             endDate: DateTime,
                             amountPaidToday: Float,
                             planAmount: Float,
                             nextPaymentPrice: Option[Float],
                             nextPaymentDate: DateTime) {

  val annual = endDate.plusDays(1) == startDate.plusYears(1)

}
