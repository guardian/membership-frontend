import com.gu.googleauth
import com.gu.identity.play.IdMinimalUser
import model.TierPricing
import play.api.Logger
import com.gu.membership.salesforce.{Member, PaidMember}
import com.gu.membership.util.Timing
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{Request, WrappedRequest}
import play.api.mvc.{Cookie, WrappedRequest}
import services._

import scala.concurrent.{ExecutionContext, Future}

package object actions {
  type AuthRequest[A] = AuthenticatedRequest[A, IdMinimalUser]

  type GoogleAuthRequest[A] = AuthenticatedRequest[A, googleauth.UserIdentity]

  trait TierPricingProvider {
    val touchpointBackend: TouchpointBackend
    def tierPricing: Future[TierPricing] = touchpointBackend.subscriptionService.tierPricing
  }

  implicit class RichAuthRequest[A](req: AuthRequest[A]) extends TierPricingProvider {
    lazy val touchpointBackend = TouchpointBackend.forUser(req.user)

    def forMemberOpt[A, T](f: Option[Member] => T)(implicit executor: ExecutionContext): Future[T] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.memberRepository.get(req.user.id)
        } yield f(memberOpt)
      }
    }

  case class MemberRequest[A, +M <: Member](member: M, request: AuthRequest[A]) extends WrappedRequest[A](request)
                                                                                with TierPricingProvider {
    val user = request.user

    def idCookies: Option[Seq[Cookie]] = for {
      guu <- request.cookies.get("GU_U")
      scguu <- request.cookies.get("SC_GU_U")
    } yield Seq(guu, scguu)

    lazy val touchpointBackend = TouchpointBackend.forUser(user)
  }

  type AnyMemberTierRequest[A] = MemberRequest[A, Member]

  type PaidMemberRequest[A] = MemberRequest[A, PaidMember]

  case class IdentityGoogleAuthRequest[A](googleUser: googleauth.UserIdentity, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val identityUser = request.user
  }
}
