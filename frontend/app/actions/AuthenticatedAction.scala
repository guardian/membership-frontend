package actions

import services.AuthenticationService
import scala.concurrent.Future
import play.api.mvc.{ Request, ActionBuilder, Result }
import controllers.NoCache
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Note: By default AuthenticatedAction applies a no-cache header on the response
 */
trait AuthenticatedAction extends ActionBuilder[AuthRequest] {

  val authService: AuthenticationService

  def invokeBlock[A](request: Request[A], block: AuthRequest[A] => Future[Result]) =
    authService.handleAuthenticatedRequest(request).fold(Future.successful, block).map { NoCache(_) }
}

object AuthenticatedAction extends AuthenticatedAction {
  val authService = AuthenticationService
}

