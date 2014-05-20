package actions

import play.api.mvc.{ WrappedRequest, Request }
import com.gu.identity.model.User

case class AuthRequest[A](request: Request[A], user: User) extends WrappedRequest(request)
