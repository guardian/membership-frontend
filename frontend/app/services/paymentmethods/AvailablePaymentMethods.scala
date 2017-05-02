package services.paymentmethods

import com.gu.identity.play.IdMinimalUser
import com.gu.zuora
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.CommonPaymentForm

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


case class InitialiserAndToken(initialiser: PaymentMethodInitialiser[_ <: zuora.soap.models.Commands.PaymentMethod], token: String) extends LazyLogging {
  def initialiseUsing(user: IdMinimalUser): Future[zuora.soap.models.Commands.PaymentMethod] = {
    val initialiserName = initialiser.getClass.getSimpleName
    logger.info(s"Initialising payment token for user ${user.id} with $initialiserName...")
    initialiser.initialiseWith(token, user).andThen {
      case Success(_) => logger.info(s"...successfully initialised payment token for user ${user.id}")
      case Failure(e) => logger.error(s"...failed to initialise payment token with $initialiserName", e)
    }
  }
}

class AvailablePaymentMethods(initialisers: Seq[PaymentMethodInitialiser[_ <: zuora.soap.models.Commands.PaymentMethod]]) {

  def deriveInitialiserAndTokenFrom(form: CommonPaymentForm): InitialiserAndToken = (for {
    initialiser <- initialisers
    token <- initialiser.extractTokenFrom(form)
  } yield InitialiserAndToken(initialiser, token)).head

}
