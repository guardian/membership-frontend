package views.support

import com.gu.memsub.{PaymentMethod, Price}
import org.joda.time.LocalDate

case class ThankYouMonthlySummary(startDate: LocalDate, amountPaidToday: Price, paymentMethod: Option[PaymentMethod])
