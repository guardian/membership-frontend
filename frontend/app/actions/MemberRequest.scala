package actions

import com.gu.identity.model.User
import play.api.mvc.{WrappedRequest, Request}
import model.Member

case class MemberRequest[A](request: Request[A], member: Member, user: User) extends WrappedRequest(request)
