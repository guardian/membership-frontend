package actions

import scala.concurrent.Future

import play.api.mvc.{Request, ActionBuilder}
import play.api.mvc.Results.Forbidden
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.{MemberService, AuthenticationService}
import controllers.NoCache
import play.api.mvc.Result

trait MemberAction extends ActionBuilder[MemberRequest] {
  val authService: AuthenticationService

  def invokeBlock[A](request: Request[A], block: MemberRequest[A] => Future[Result]) = {
    authService.authenticatedRequestFor(request).map { authRequest =>
      for {
        member <- MemberService.get(authRequest.user.id)
        result <- block(MemberRequest[A](request, member, authRequest.user))
      } yield NoCache(result)
    }.getOrElse(Future.successful(Forbidden))
  }
}

object MemberAction extends MemberAction {
  val authService = AuthenticationService
}

