package services.paymentmethods

import com.gu.identity.play.IdMinimalUser
import com.gu.zuora
import forms.MemberForm.CommonPaymentForm

import scala.concurrent.Future


case class InitialiserAndToken(initialiser: PaymentMethodInitialiser[_ <: zuora.soap.models.Commands.PaymentMethod], token: String) {
  def initialiseUsing(user: IdMinimalUser): Future[zuora.soap.models.Commands.PaymentMethod] = {
    initialiser.initialiseWith(token, user)
  }
}

class AvailablePaymentMethods(initialisers: Seq[PaymentMethodInitialiser[_ <: zuora.soap.models.Commands.PaymentMethod]]) {

  def deriveInitialiserAndTokenFrom(form: CommonPaymentForm): InitialiserAndToken = (for {
    initialiser <- initialisers
    token <- initialiser.extractTokenFrom(form)
  } yield InitialiserAndToken(initialiser, token)).head

}
