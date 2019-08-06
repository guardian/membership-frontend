package services.checkout.identitystrategy

import com.gu.identity.model.{User => IdUser}
import controllers.IdentityRequest
import forms.MemberForm.CommonForm
import play.api.mvc.{RequestHeader, Result}
import services.{AuthenticationService, IdentityService}

import scala.concurrent.{ExecutionContext, Future}

class StrategyDecider(authenticationService: AuthenticationService) {

  def identityStrategyFor(identityService: IdentityService, request: RequestHeader, form: CommonForm): Strategy = {
    implicit val idService = identityService
    implicit val idRequest = IdentityRequest(request)

    (for (user <- authenticationService.authenticateUser(request))
      yield ExistingSignedInUser(user.minimalUser, form)).getOrElse(NewUser.strategyFrom(form).get)
  }
}

trait Strategy {
  def ensureIdUser(checkoutFunc: (IdUser) => Future[Result])(implicit executionContext: ExecutionContext): Future[Result]
}
