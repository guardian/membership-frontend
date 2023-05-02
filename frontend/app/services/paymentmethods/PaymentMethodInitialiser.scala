package services.paymentmethods

import com.gu.i18n.{Country, CountryGroup}
import com.gu.monitoring.SafeLogger
import com.gu.zuora
import com.gu.zuora.soap.models.Commands.CreditCardReferenceTransaction
import forms.MemberForm.CommonPaymentForm
import model.IdMinimalUser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

/** An initialiser just takes a token from the Payment form and sends it on to the 3rd-party
  * payment-processor (PayPal, Stripe, GoCardless) to exchange for a summary of the customer's
  * payment method. This payment method summary can then be sent on to Zuora for charging.
  */
trait PaymentMethodInitialiser[PMCommand <: zuora.soap.models.Commands.PaymentMethod] {

  // at some point, the token may be more complicated, but right now it's always a string
  def extractTokenFrom(form: CommonPaymentForm): Option[String]

  def initialiseWith(token: String, user: IdMinimalUser)(implicit executionContext: ExecutionContext): Future[PMCommand]

  def appliesToCountry(country: Country): Boolean
}



