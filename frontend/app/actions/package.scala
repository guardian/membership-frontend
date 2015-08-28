import com.gu.membership.salesforce.{Member, PaidMember}
import com.gu.membership.util.Timing
import com.gu.googleauth
import com.gu.identity.play.IdMinimalUser
import play.api.Logger
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.WrappedRequest
import services._

import scala.concurrent.{ExecutionContext, Future}

package object actions {

  val logger = Logger(this.getClass())

  type AuthRequest[A] = AuthenticatedRequest[A, IdMinimalUser]

  type GoogleAuthRequest[A] = AuthenticatedRequest[A, googleauth.UserIdentity]

  implicit class RichAuthRequest[A](req: AuthRequest[A]) {
    lazy val touchpointBackend = TouchpointBackend.forUser(req.user)

    def forMemberOpt[A, T](f: Option[Member] => T)(implicit executor: ExecutionContext): Future[T] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.memberRepository.get(req.user.id)
        } yield f(memberOpt)
      }
    }

  case class MemberRequest[A, +M <: Member](member: M, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val user = request.user

    lazy val touchpointBackend = TouchpointBackend.forUser(user)
  }

  type AnyMemberTierRequest[A] = MemberRequest[A, Member]

  type PaidMemberRequest[A] = MemberRequest[A, PaidMember]

  case class IdentityGoogleAuthRequest[A](val googleUser: googleauth.UserIdentity, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val identityUser = request.user
  }
}
