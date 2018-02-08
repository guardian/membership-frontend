import com.gu.googleauth
import com.gu.identity.play.AuthenticatedIdUser
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
    def touchpointBackend(implicit touchpointBackend: TouchpointBackends): TouchpointBackend
  }

  implicit class RichAuthRequest[A](req: AuthRequest[A]) extends BackendProvider {
    def touchpointBackend(implicit tbp: TouchpointBackends) = tbp.forUser(req.user)

    def forMemberOpt(implicit executor: ExecutionContext, tbp: TouchpointBackends): Future[String \/ Option[GenericSFContact]] =
      Timing.record(MemberAuthenticationMetrics, s"${req.method} ${req.path}") {
        for {
          memberOpt <- touchpointBackend.salesforceService.getMember(req.user.id)
        } yield memberOpt
      }
    }

  case class SubscriptionRequest[A](tb: TouchpointBackend,
                                    request: AuthRequest[A]) extends AuthRequest(request.user, request) with BackendProvider {

    def touchpointBackend(implicit tbp: TouchpointBackends) = tb

    def this(other: SubscriptionRequest[A]) =
      this(other.tb, other.request)
  }

  case class OptionallyAuthenticatedRequest[A](tb: TouchpointBackend, user: Option[AuthenticatedIdUser], request: Request[A])
    extends WrappedRequest[A](request) with BackendProvider {
    def touchpointBackend(implicit tbp: TouchpointBackends) = tb
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
    def touchpointBackend(implicit touchpointBackend: TouchpointBackends): TouchpointBackend = touchpointBackend.forUser(request.user)
    val identityUser = request.user
  }
}
