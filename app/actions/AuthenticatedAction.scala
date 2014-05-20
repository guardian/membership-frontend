package actions

import services.AuthenticationService
import scala.concurrent.Future
import play.api.mvc.{ Request, ActionBuilder, SimpleResult }

trait AuthenticatedAction extends ActionBuilder[AuthRequest] {

  val authService: AuthenticationService

  protected def invokeBlock[A](request: Request[A], block: AuthRequest[A] => Future[SimpleResult]) =
    authService.handleAuthenticatedRequest(request).fold(Future.successful, block)
}

object AuthenticatedAction extends AuthenticatedAction {
  val authService = AuthenticationService
}

