package services.paymentmethods

import com.gu.i18n.Country
import model.IdMinimalUser
import com.gu.zuora
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import forms.MemberForm.CommonPaymentForm
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


case class InitialiserAndToken(initialiser: PaymentMethodInitialiser[_ <: zuora.soap.models.Commands.PaymentMethod], token: String) {
  def initialiseUsing(user: IdMinimalUser)(implicit executionContext: ExecutionContext): Future[zuora.soap.models.Commands.PaymentMethod] = {
    val initialiserName = initialiser.getClass.getSimpleName
    SafeLogger.info(s"Initialising payment token for user ${user.id} with $initialiserName...")
    initialiser.initialiseWith(token, user).andThen {
      case Success(_) => SafeLogger.info(s"...successfully initialised payment token for user ${user.id}")
      case Failure(e) => SafeLogger.error(scrub"...failed to initialise payment token with $initialiserName", e)
    }
  }
}

class AvailablePaymentMethods(initialisers: Set[PaymentMethodInitialiser[_ <: zuora.soap.models.Commands.PaymentMethod]]) {

  def deriveInitialiserAndTokenFrom(form: CommonPaymentForm, transactingCountry: Country)(implicit executionContext: ExecutionContext): InitialiserAndToken = (for {
    initialiser <- initialisers
    token <- initialiser.extractTokenFrom(form)
  } yield InitialiserAndToken(initialiser, token))
    .filter(_.initialiser.appliesToCountry(transactingCountry))
    .head
}
