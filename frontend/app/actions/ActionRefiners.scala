package actions

import _root_.services._
import actions.Fallbacks._
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.{Subscription, _}
import com.gu.memsub.util.Timing
import com.gu.memsub.{Status => SubStatus, Subscription => Sub, _}
import com.gu.monitoring.CloudWatch
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import controllers.IdentityRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._
import utils.PlannedOutage
import views.support.MembershipCompat._

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
  type SubRequestWithContributorOrResult[A] = Future[Either[Result, SubReqWithContributor[A]]]
  type PaidSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with PaidSubscriber]]
  type FreeSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with FreeSubscriber]]

  type SubReqWithPaid[A] = SubscriptionRequest[A] with PaidSubscriber
  type SubReqWithFree[A] = SubscriptionRequest[A] with FreeSubscriber
  type SubReqWithSub[A] = SubscriptionRequest[A] with Subscriber
  type SubReqWithContributor[A] = SubscriptionRequest[A] with Contributor

  private def getContributorRequest[A](request: AuthRequest[A]): Future[Option[SubReqWithContributor[A]]] = {
    implicit val pf = Membership
    val tp = request.touchpointBackend
    val contributor = Subscriber[Subscription[SubscriptionPlan.Contributor]] _
    (for {
      member <- OptionT(request.forMemberOpt(identity))
      subscription <- OptionT(tp.subscriptionService.getSubscription(member))
    } yield new SubscriptionRequest[A](tp, request) with Contributor {
      override val contributor = subscription.plan
    }).run
  }

  private def getSubRequest[A](request: Request[A]): Future[Option[SubReqWithSub[A]]] = {
    implicit val pf = Membership
    val FreeSubscriber = Subscriber[Subscription[SubscriptionPlan.FreeMember]] _
    val PaidSubscriber = Subscriber[Subscription[SubscriptionPlan.PaidMember]] _
    (for {
      user <- OptionT(Future.successful(AuthenticationService.authenticatedIdUserProvider(request)))
      authRequest = new AuthenticatedRequest(user, request)
      tp = authRequest.touchpointBackend
      member <- OptionT(authRequest.forMemberOpt(identity))
      subscription <- OptionT(tp.subscriptionService.either[SubscriptionPlan.FreeMember, SubscriptionPlan.PaidMember](member))
    } yield new SubscriptionRequest[A](tp, authRequest) with Subscriber {
      override def paidOrFreeSubscriber = subscription.bimap(FreeSubscriber(_, member), PaidSubscriber(_, member))
    }).run
  }

  val PlannedOutageProtection = new ActionFilter[Request] {
    override def filter[A](request: Request[A]) = Future.successful(PlannedOutage.currentOutage.map(_ => maintenance(request)))
  }

  def subscriptionRefiner(onNonMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[AuthRequest, SubReqWithSub] {
    override def refine[A](request: AuthRequest[A]): SubRequestOrResult[A] = {
      getSubRequest(request).map(_ toRight onNonMember(request))
    }
  }

  def contributionRefiner(onNonContributor: RequestHeader => Result = contributorJoinRedirect(_)) = new ActionRefiner[AuthRequest, SubReqWithContributor] {
    override def refine[A](request: AuthRequest[A]): SubRequestWithContributorOrResult[A] = {
      getContributorRequest(request).map(_ toRight onNonContributor(request))
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
      val currentSubscription = request.subscriber.subscription
      val memberService = request.touchpointBackend.memberService
      Future.successful {
        if (!memberService.upgradeTierIsValidForCurrency(currentSubscription, targetTier)) {
          Some(Ok(views.html.tier.upgrade.unavailableTierForCurrency(currentSubscription.plan.tier, targetTier)))
        } else if (!memberService.upgradeTierIsHigher(currentSubscription, targetTier)) {
          Some(Ok(views.html.tier.upgrade.unavailableUpgradePath(currentSubscription.plan.tier, targetTier)))
        } else {
          None
        }
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
      Ok(views.html.tier.upgrade.unavailableUpgradePath(req.subscriber.subscription.plan.tier, selectedTier))
    }
  }

  def noAuthenticatedMemberFilter(onMember: SubReqWithSub[_] => Result = memberHome(_)) = new ActionFilter[Request] {
    override def filter[A](request: Request[A]) = getSubRequest(request).map(_.map(onMember))
  }

  def onlyNonMemberFilter(onMember: SubReqWithSub[_] => Result = memberHome(_)) = new ActionFilter[AuthRequest] {
    override def filter[A](request: AuthRequest[A]) = getSubRequest(request).map(_.map(onMember))
  }

  def onlyNonContributorFilter(onContributor: SubReqWithContributor[_] => Result = memberHome(_)) = new ActionFilter[AuthRequest] {
    override def filter[A](request: AuthRequest[A]) = getContributorRequest(request).map(_.map(onContributor))
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
