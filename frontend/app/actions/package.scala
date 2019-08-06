import com.gu.googleauth
import model.AuthenticatedIdUser
import com.gu.memsub.Subscriber.{FreeMember, Member, PaidMember}
import com.gu.memsub.subsv2.{Subscription, SubscriptionPlan}
import com.gu.salesforce._
import com.gu.memsub.util.Timing
import model.GenericSFContact
import monitoring.MemberAuthenticationMetrics
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{Cookie, Request, WrappedRequest}
import services._

import scalaz.\/
import scala.concurrent.{ExecutionContext, Future}

package object actions {

  type AuthRequest[A] = AuthenticatedRequest[A, AuthenticatedIdUser]
  type GoogleAuthRequest[A] = AuthenticatedRequest[A, googleauth.UserIdentity]

  trait BackendProvider {
    def touchpointBackend(implicit touchpointBackends: TouchpointBackends): TouchpointBackend
  }

  implicit class RichAuthRequest[A](req: AuthRequest[A]) extends BackendProvider {
    def touchpointBackend(implicit tpbs: TouchpointBackends) = tpbs.forUser(req.user.minimalUser)

    def forMemberOpt(implicit executor: ExecutionContext, tpbs: TouchpointBackends): Future[String \/ Option[GenericSFContact]] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.salesforceService.getMember(req.user.minimalUser.id)
        } yield memberOpt
      }
    }

  case class SubscriptionRequest[A](tpb: TouchpointBackend,
                                    request: AuthRequest[A]) extends AuthRequest(request.user, request) with BackendProvider {

    def touchpointBackend(implicit tpbs: TouchpointBackends) = tpb

    def this(other: SubscriptionRequest[A]) =
      this(other.tpb, other.request)
  }

  case class OptionallyAuthenticatedRequest[A](tpb: TouchpointBackend, user: Option[AuthenticatedIdUser], request: Request[A])
    extends WrappedRequest[A](request) with BackendProvider {
    def touchpointBackend(implicit tpbs: TouchpointBackends) = tpb
  }

  trait Subscriber {
    def paidOrFreeSubscriber: FreeMember \/ PaidMember
    def subscriber: Member = paidOrFreeSubscriber.fold[Member](identity, identity)
  }

  trait PaidSubscriber {
    def subscriber: PaidMember
  }

  trait Contributor {
    def contributor: Subscription[SubscriptionPlan.Contributor]
  }

  trait FreeSubscriber {
    def subscriber: FreeMember
  }

  case class IdentityGoogleAuthRequest[A](googleUser: googleauth.UserIdentity, request: AuthRequest[A]) extends WrappedRequest[A](request) with BackendProvider {
    def touchpointBackend(implicit touchpointBackends: TouchpointBackends): TouchpointBackend = touchpointBackends.forUser(request.user.minimalUser)
    val identityUser = request.user
  }
}
