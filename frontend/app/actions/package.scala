import com.gu.googleauth
import com.gu.identity.play.AuthenticatedIdUser
import com.gu.membership.salesforce._
import com.gu.membership.util.Timing
import model.MembershipCatalog
import monitoring.MemberAuthenticationMetrics
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{Cookie, WrappedRequest}
import services._

import scala.concurrent.{ExecutionContext, Future}

package object actions {
  type AuthRequest[A] = AuthenticatedRequest[A, AuthenticatedIdUser]

  type GoogleAuthRequest[A] = AuthenticatedRequest[A, googleauth.UserIdentity]

  trait TierDetailsProvider {
    def touchpointBackend: TouchpointBackend
    def catalog: Future[MembershipCatalog] = touchpointBackend.subscriptionService.membershipCatalog.get()
  }

  implicit class RichAuthRequest[A](req: AuthRequest[A]) extends TierDetailsProvider {
    lazy val touchpointBackend = TouchpointBackend.forUser(req.user)

    def forMemberOpt[T](f: Option[Contact[Member, PaymentMethod]] => T)(implicit executor: ExecutionContext): Future[T] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.memberRepository.getMember(req.user.id)
        } yield f(memberOpt)
      }
    }

  case class MemberRequest[A, +M <: Contact[Member, PaymentMethod]](member: M, request: AuthRequest[A]) extends WrappedRequest[A](request)
                                                                                                        with TierDetailsProvider {
    val user = request.user

    def idCookies: Option[Seq[Cookie]] = for {
      guu <- request.cookies.get("GU_U")
      scguu <- request.cookies.get("SC_GU_U")
    } yield Seq(guu, scguu)

    lazy val touchpointBackend = TouchpointBackend.forUser(user)
  }

  case class SubscriptionRequest[A](subscription: model.Subscription,
                                    memberRequest: MemberRequest[A, Contact[Member, PaymentMethod]]
                                   ) extends WrappedRequest[A](memberRequest) with TierDetailsProvider {
    val user = memberRequest.user
    val touchpointBackend = memberRequest.touchpointBackend
    val member = memberRequest.member
  }

  type AnyMemberTierRequest[A] = MemberRequest[A, Contact[Member, PaymentMethod]]

  type PaidMemberRequest[A] = MemberRequest[A, Contact[PaidTierMember, StripePayment]]

  case class IdentityGoogleAuthRequest[A](googleUser: googleauth.UserIdentity, request: AuthRequest[A]) extends WrappedRequest[A](request) {
    val identityUser = request.user
  }
}
