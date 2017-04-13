package services.checkout.identitystrategy

import com.gu.identity.play.IdUser
import controllers.IdentityRequest
import forms.MemberForm.CommonForm
import play.api.mvc.{RequestHeader, Result}
import services.{IdentityApi, IdentityService}

import scala.concurrent.Future

object Strategy {
  val identityService = IdentityService(IdentityApi)

  def identityStrategyFor(request: RequestHeader, form: CommonForm): Strategy = {
    implicit val idRequest = IdentityRequest(request)

    val strategies = ExistingSignedInUser.strategyFrom(request, form) ++ NewUser.strategyFrom(form)

    assert(strategies.size == 1, s"Should have exactly 1 CheckoutIdentityStrategy, instead have ${strategies.size}")

    strategies.head
  }
}

trait Strategy {
  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result]): Future[Result]
}
