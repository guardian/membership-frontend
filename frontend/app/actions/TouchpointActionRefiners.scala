package actions

import actions.ActionRefiners.{SubReqWithContributor, SubReqWithSub, SubRequestOrResult, SubRequestWithContributorOrResult, logger}
import actions.Fallbacks.{memberHome, notYetAMemberOn}
import com.gu.memsub.{Membership, Subscriber => MemSubscriber}
import com.gu.memsub.subsv2.SubscriptionPlan
import com.gu.salesforce.PaidTier
import play.api.mvc.Results.{InternalServerError, Ok}
import services.{AuthenticationService, TouchpointBackendProvider}
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.{Subscription, _}
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import views.support.MembershipCompat._

import scalaz.{-\/, EitherT, OptionT, \/, \/-}
import scalaz.std.scalaFuture._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

class TouchpointActionRefiners(touchpointBackend: TouchpointBackendProvider, executionContext: ExecutionContext) extends LazyLogging {

  implicit private val ec = executionContext
  implicit private val tpb = touchpointBackend

  private def getContributorRequest[A](request: AuthRequest[A]): Future[String \/ Option[SubReqWithContributor[A]]] = {
    implicit val pf = Membership
    val tp = request.touchpointBackend
    val contributor = MemSubscriber[Subscription[SubscriptionPlan.Contributor]] _
    (for {
      member <- OptionEither(request.forMemberOpt)
      subscription <- OptionEither.liftEither(tp.subscriptionService.getSubscription(member))
    } yield new SubscriptionRequest[A](tp, request) with Contributor {
      override val contributor = subscription
    }).run.run
  }

  private def getSubRequest[A](request: Request[A]): Future[String \/ Option[SubReqWithSub[A]]] = {
    implicit val pf = Membership
    val FreeSubscriber = MemSubscriber[Subscription[SubscriptionPlan.FreeMember]] _
    val PaidSubscriber = MemSubscriber[Subscription[SubscriptionPlan.PaidMember]] _
    (for {
      user <- OptionEither.liftFutureEither(AuthenticationService.authenticatedIdUserProvider(request))
      authRequest = new AuthenticatedRequest(user, request)
      tp = authRequest.touchpointBackend
      member <- OptionEither(authRequest.forMemberOpt)
      subscription <- OptionEither.liftEither(tp.subscriptionService.either[SubscriptionPlan.FreeMember, SubscriptionPlan.PaidMember](member).map(_.toOption.flatten))
    } yield new SubscriptionRequest[A](tp, authRequest) with Subscriber {
      override def paidOrFreeSubscriber = subscription.bimap(FreeSubscriber(_, member), PaidSubscriber(_, member))
    }).run.run
  }

  def subscriptionRefiner(onNonMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[AuthRequest, SubReqWithSub] {
    override def refine[A](request: AuthRequest[A]): SubRequestOrResult[A] = {
      getSubRequest(request).map {
        case -\/(message) =>
          logger.warn(s"error while sub refining: $message")
          Left(InternalServerError(views.html.error500(new Throwable)))
        case \/-(maybeMember) => maybeMember toRight onNonMember(request)}
    }
  }

  def contributionRefiner(onNonContributor: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[AuthRequest, SubReqWithContributor] {
    override def refine[A](request: AuthRequest[A]): SubRequestWithContributorOrResult[A] = {
      getContributorRequest(request).map {
        case -\/(message) =>
          logger.warn(s"error while contribution refining: $message")
          Left(InternalServerError(views.html.error500(new Throwable)))
        case \/-(maybeMember) => maybeMember toRight onNonContributor(request)
      }
    }
  }

  def noAuthenticatedMemberFilter(onMember: SubReqWithSub[_] => Result = memberHome(_)) = new ActionFilter[Request] {
    override def filter[A](request: Request[A]) =
      getSubRequest(request).map {
        case -\/(message) =>
          logger.warn(s"error while filtering: $message")
          Some(InternalServerError(views.html.error500(new Throwable)))
        case \/-(maybeSub) => maybeSub.map(onMember)
      }
  }

  def onlyNonMemberFilter(onMember: SubReqWithSub[_] => Result = memberHome(_)) = new ActionFilter[AuthRequest] {
    override def filter[A](request: AuthRequest[A]) =
      getSubRequest(request).map {
        case -\/(message) =>
          logger.warn(s"error while filtering: $message")
          Some(InternalServerError(views.html.error500(new Throwable)))
        case \/-(maybeSub) => maybeSub.map(onMember)
      }
  }

  def onlyNonContributorFilter(onContributor: SubReqWithContributor[_] => Result = memberHome(_)) = new ActionFilter[AuthRequest] {
    override def filter[A](request: AuthRequest[A]) =
      getContributorRequest(request).map {
        case -\/(message) =>
          logger.warn(s"error while filtering contributors: $message")
          Some(InternalServerError(views.html.error500(new Throwable)))
        case \/-(maybeSub) => maybeSub.map(onContributor)
      }
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
}
