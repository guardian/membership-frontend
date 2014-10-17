import com.gu.identity.model.User
import com.gu.membership.salesforce.{Member, PaidMember, Tier}
import com.gu.membership.util.Timing
import configuration.Config
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.WrappedRequest
import services.MemberRepository

import scala.concurrent.{ExecutionContext, Future}

package object actions {
  type AuthRequest[A] = AuthenticatedRequest[A, User]

  implicit class RichAuthRequest[A](req: AuthRequest[A]) {
    def forMemberOpt[A, T](f: Option[Member] => T)(implicit executor: ExecutionContext): Future[T] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- MemberRepository.get(req.user.id).map(_.filter(_.tier > Tier.None))
        } yield f(memberOpt)
      }
    }

  implicit class RichUser(user: User) {
    lazy val isTestUser: Boolean = {
      user.publicFields.displayName.map(dn => Config.testUsers.validate(dn.split(' ')(0)).isDefined).getOrElse(false)
    }
  }

  case class MemberRequest[A, +M <: Member](val member: M, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val user = request.user
  }

  type AnyMemberTierRequest[A] = MemberRequest[A, Member]

  type PaidMemberRequest[A] = MemberRequest[A, PaidMember]
}
