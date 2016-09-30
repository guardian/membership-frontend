package actions

import actions.Fallbacks._
import com.gu.googleauth.UserIdentity
import com.gu.membership.{FreeMembershipPlan, PaidMembershipPlan}
import com.gu.memsub.Subscriber.{Member, FreeMember, PaidMember}
import com.gu.memsub.Subscription.{FreeMembershipSub, PaidMembershipSub}
import com.gu.memsub.util.Timing
import configuration.Config.googleGroupChecker
import services._
import com.gu.memsub.{Status => SubStatus, Subscription => Sub, _}
import com.gu.monitoring.CloudWatch
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import controllers.IdentityRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._

import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.scalaFuture._


/**
 * These ActionFunctions serve as components that can be composed to build the
 * larger, more-generally useful pipelines in 'CommonActions'.
 *
 * https://www.playframework.com/documentation/2.3.x/ScalaActionsComposition
 */
object ActionRefiners extends LazyLogging {
  import model.TierOrdering.upgradeOrdering
  implicit val pf: ProductFamily = Membership

  def resultModifier(f: Result => Result) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  def authenticated(onUnauthenticated: RequestHeader => Result = chooseRegister(_)): ActionBuilder[AuthRequest] =
    new AuthenticatedBuilder(AuthenticationService.authenticatedIdUserProvider, onUnauthenticated)

  type SubRequestOrResult[A] = Future[Either[Result, SubReqWithSub[A]]]
  type PaidSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with PaidSubscriber]]
  type FreeSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with FreeSubscriber]]

  type SubReqWithPaid[A] = SubscriptionRequest[A] with PaidSubscriber
  type SubReqWithFree[A] = SubscriptionRequest[A] with FreeSubscriber
  type SubReqWithSub[A] = SubscriptionRequest[A] with Subscriber

  private def getSubRequest[A](request: AuthRequest[A]): Future[Option[SubReqWithSub[A]]] = {
    implicit val pf = Membership
    val tp = request.touchpointBackend
    val FreeSubscriber = Subscriber[FreeMembershipSub] _
    val PaidSubscriber = Subscriber[PaidMembershipSub] _

    (for {
      member <- OptionT(request.forMemberOpt(identity))
      subscription <- OptionT(tp.subscriptionService.getEither(member))
    } yield new SubscriptionRequest[A](tp, request) with Subscriber {
      override def paidOrFreeSubscriber = subscription.bimap(FreeSubscriber(_, member), PaidSubscriber(_, member))
    }).run
  }

  def subscriptionRefiner(onNonMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[AuthRequest, SubReqWithSub] {
    override def refine[A](request: AuthRequest[A]): SubRequestOrResult[A] = {
      getSubRequest(request).map(_ toRight onNonMember(request))
    }
  }

  def paidSubscriptionRefiner(onFreeMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[SubReqWithSub, SubReqWithPaid] {
    override protected def refine[A](request: SubReqWithSub[A]): Future[Either[Result, SubReqWithPaid[A]]] =
      Future.successful(request.paidOrFreeSubscriber.bimap(free => onFreeMember(request), paid =>
        new SubscriptionRequest[A](request) with PaidSubscriber {
          override def subscriber = paid
        }).toEither
      )
  }

  def freeSubscriptionRefiner(onPaidMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[SubReqWithSub, SubReqWithFree] {
    override protected def refine[A](request: SubReqWithSub[A]): Future[Either[Result, SubReqWithFree[A]]] =
      Future.successful(request.paidOrFreeSubscriber.bimap(free =>
        new SubscriptionRequest[A](request) with FreeSubscriber {
          override def subscriber = free
        }, _ => onPaidMember(request)).swap.toEither // we convert paid to a response, thus giving Free \/ Response
      )                                              // but we need Either[Response, Free], hence the swap and toEither
  }

  def checkTierChangeTo(targetTier: PaidTier) = new ActionFilter[SubReqWithSub] {
    override protected def filter[A](request: SubReqWithSub[A]): Future[Option[Result]] = {
      if (!request.touchpointBackend.memberService.subscriptionUpgradableTo(request.subscriber.subscription, targetTier)) {
        Future.successful(Some(Ok(views.html.tier.upgrade.unavailable(request.subscriber.subscription.plan.tier, targetTier))))
      } else {
        Future.successful(None)
      }
    }
  }

  def redirectMemberAttemptingToSignUp(selectedTier: Tier)(req: SubReqWithSub[_]): Result = selectedTier match {
    case t: PaidTier if t > req.subscriber.subscription.plan.tier => tierChangeEnterDetails(t)(req)
    case _ => {
      // Log cancelled members attempting to re-join.
      if (req.subscriber.subscription.isCancelled) {
        logger.info(s"Cancelled member with ID: ${req.subscriber.contact.identityId} attempted to re-join.")
      }
      Ok(views.html.tier.upgrade.unavailable(req.subscriber.subscription.plan.tier, selectedTier))
    }
  }

  def onlyNonMemberFilter(onMember: SubReqWithSub[_] => Result = memberHome(_)) = new ActionFilter[AuthRequest] {
    override def filter[A](request: AuthRequest[A]) = getSubRequest(request).map(_.map(onMember))
  }

  def googleAuthenticationRefiner(onNonAuthentication: RequestHeader => Result = OAuthActions.sendForAuth) = {
    new ActionRefiner[AuthRequest, IdentityGoogleAuthRequest] {
      override def refine[A](request: AuthRequest[A]) = Future.successful {
        OAuthActions.userIdentity(request).map(IdentityGoogleAuthRequest(_, request)).toRight(onNonAuthentication(request))
      }
    }
  }

  def matchingGuardianEmail(onNonGuEmail: RequestHeader => Result =
                            joinStaffMembership(_).flashing("error" -> "Identity email must match Guardian email")) = new ActionFilter[IdentityGoogleAuthRequest] {
    override def filter[A](request: IdentityGoogleAuthRequest[A]) = {
      for {
        user <- IdentityService(IdentityApi).getFullUserDetails(request.identityUser)(IdentityRequest(request))
      } yield {
        if (GuardianDomains.emailsMatch(request.googleUser.email, user.primaryEmailAddress)) None
        else Some(onNonGuEmail(request))
      }
    }
  }

  def metricRecord(cloudWatch: CloudWatch, metricName: String) = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      Timing.record(cloudWatch, metricName) {
        block(request)
      }
  }
}
