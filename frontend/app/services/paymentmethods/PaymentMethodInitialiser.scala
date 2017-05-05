package services.paymentmethods

import com.gu.i18n.CountryGroup
import com.gu.identity.play.IdMinimalUser
import com.gu.stripe.StripeService
import com.gu.zuora
import com.gu.zuora.soap.models.Commands.{CreditCardReferenceTransaction, PayPalReferenceTransaction}
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.CommonPaymentForm
import services.PayPalService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

/** An initialiser just takes a token from the Payment form and sends it on to the 3rd-party
  * payment-processor (PayPal, Stripe, GoCardless) to exchange for a summary of the customer's
  * payment method. This payment method summary can then be sent on to Zuora for charging.
  */
trait PaymentMethodInitialiser[PMCommand <: zuora.soap.models.Commands.PaymentMethod] {

  // at some point, the token may be more complicated, but right now it's always a string
  def extractTokenFrom(form: CommonPaymentForm): Option[String]

  def initialiseWith(token: String, user: IdMinimalUser): Future[PMCommand]
}

class StripeInitialiser(stripeService: StripeService) extends
  PaymentMethodInitialiser[CreditCardReferenceTransaction] with LazyLogging {

  def extractTokenFrom(form: CommonPaymentForm): Option[String] = form.stripeToken

  def initialiseWith(stripeToken: String, user: IdMinimalUser): Future[CreditCardReferenceTransaction] = for {
    stripeCustomer <- stripeService.Customer.create(user.id, stripeToken).andThen {
      case Failure(e) => logger.warn(s"Could not create Stripe customer for user ${user.id}", e)
    }
  } yield {
    val card = stripeCustomer.card
    CreditCardReferenceTransaction(card.id, stripeCustomer.id, card.last4, CountryGroup.countryByCode(card.country), card.exp_month, card.exp_year, card.`type`)
  }
}

class PayPalInitialiser(payPalService: PayPalService) extends PaymentMethodInitialiser[PayPalReferenceTransaction] {

  def extractTokenFrom(form: CommonPaymentForm): Option[String] = form.payPalBaid

  def initialiseWith(baid: String, user: IdMinimalUser): Future[PayPalReferenceTransaction] = for {
    payPalEmail <- payPalService.retrieveEmail(baid)
  } yield PayPalReferenceTransaction(baid, payPalEmail)

}
