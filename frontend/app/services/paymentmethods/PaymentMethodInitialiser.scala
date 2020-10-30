package services.paymentmethods

import com.gu.i18n.{Country, CountryGroup}
import model.IdMinimalUser
import com.gu.stripe.StripeService
import com.gu.zuora
import com.gu.zuora.api.RegionalStripeGateways
import com.gu.zuora.soap.models.Commands.{CreditCardReferenceTransaction, PayPalReferenceTransaction}
import com.gu.monitoring.SafeLogger
import forms.MemberForm.CommonPaymentForm

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

class StripeInitialiser(stripeService: StripeService) extends
  PaymentMethodInitialiser[CreditCardReferenceTransaction] {

  def extractTokenFrom(form: CommonPaymentForm): Option[String] = form.stripeToken

  def initialiseWith(stripeToken: String, user: IdMinimalUser)(implicit executionContext: ExecutionContext): Future[CreditCardReferenceTransaction] = {
    for {
      stripeCustomer <- stripeService.Customer.create(stripeToken).andThen {
        case Failure(e) => SafeLogger.warn(s"Could not create Stripe customer for user ${user.id}", e)
      }
    } yield {
      val card = stripeCustomer.card
      CreditCardReferenceTransaction(card.id, stripeCustomer.id, card.last4, CountryGroup.countryByCode(card.country), card.exp_month, card.exp_year, card.`type`)
    }
  }

  def appliesToCountry(country: Country): Boolean = RegionalStripeGateways.getGatewayForCountry(country) == stripeService.paymentGateway
}

