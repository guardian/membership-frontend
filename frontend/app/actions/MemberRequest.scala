package actions

import com.gu.identity.model.User
import com.gu.membership.salesforce.{PaidMember, Member}
import play.api.mvc.{WrappedRequest, Request}

case class MemberRequest[A](request: Request[A], member: Member, user: User) extends WrappedRequest(request)

case class PaidMemberRequest[A](request: Request[A], member: PaidMember, user: User) extends WrappedRequest(request)
