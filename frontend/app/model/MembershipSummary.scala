package model

import org.joda.time.DateTime

case class MembershipSummary(startDate: DateTime,
                             firstPaymentEndDate: DateTime,
                             amountPaidToday: Float,
                             planAmount: Float,
                             nextPaymentPrice: Float,
                             nextPaymentDate: DateTime,
                             initialFreePeriodOffer: Boolean) {


  val annual = {
    if (initialFreePeriodOffer) nextPaymentPrice % planAmount != 0
    else firstPaymentEndDate.plusDays(1) == startDate.plusYears(1)
  }

}
