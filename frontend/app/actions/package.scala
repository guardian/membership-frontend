import com.gu.identity
import com.gu.membership.salesforce.{Member, PaidMember, Tier}
import com.gu.membership.util.Timing
import play.api.Logger
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.WrappedRequest
import services._
import utils.TestUsers

import scala.concurrent.{ExecutionContext, Future}

package object actions {

  val logger = Logger(this.getClass())

  type AuthRequest[A] = AuthenticatedRequest[A, identity.model.User]

  implicit class RichAuthRequest[A](req: AuthRequest[A]) {
    lazy val touchpointBackend = TouchpointBackend.forUser(req.user)

    def forMemberOpt[A, T](f: Option[Member] => T)(implicit executor: ExecutionContext): Future[T] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.memberRepository.get(req.user.id).map(_.filter(_.tier > Tier.None))
        } yield f(memberOpt)
      }
    }

  implicit class RichUser(user: identity.model.User) {
    lazy val isTestUser: Boolean = {
      val isValidTestUser = TestUsers.validate(user)
      if (isValidTestUser) {
        Logger.info(s"${user.getPrimaryEmailAddress} is a valid test user")
      }
      isValidTestUser
    }
  }

  case class MemberRequest[A, +M <: Member](val member: M, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val user = request.user

    lazy val touchpointBackend = TouchpointBackend.forUser(user)
  }

  type AnyMemberTierRequest[A] = MemberRequest[A, Member]

  type PaidMemberRequest[A] = MemberRequest[A, PaidMember]
}
