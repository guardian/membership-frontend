package actions

import play.api.mvc.{WrappedRequest, Request}
import model.Member

case class MemberRequest[A](request: Request[A], member: Member) extends WrappedRequest(request)
