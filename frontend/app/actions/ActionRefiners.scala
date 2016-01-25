package actions

import services._
import actions.Fallbacks._
import com.gu.googleauth.{GoogleGroupChecker, UserIdentity}
import com.gu.membership.{FreeMembershipPlan, PaidMembershipPlan}
import com.gu.memsub.util.Timing
import com.gu.memsub.Subscriber.{FreeMember, PaidMember}
import com.gu.memsub.{Subscription => Sub, Status => SubStatus, _}
import com.gu.monitoring.CloudWatch
import com.gu.salesforce._
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.IdentityRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc._
import play.twirl.api.Html

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
    new AuthenticatedBuilder(AuthenticationService.authenticatedUserFor, onUnauthenticated)

  type SubRequestOrResult[A] = Future[Either[Result, SubReqWithSub[A]]]
  type PaidSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with PaidSubscriber]]
  type FreeSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with FreeSubscriber]]

  type SubReqWithPaid[A] = SubscriptionRequest[A] with PaidSubscriber
  type SubReqWithFree[A] = SubscriptionRequest[A] with FreeSubscriber
  type SubReqWithSub[A] = SubscriptionRequest[A] with Subscriber

  private def getSubRequest[A](request: AuthRequest[A]): Future[Option[SubReqWithSub[A]]] = {
    implicit val pf = Membership
    val tp = request.touchpointBackend

    (for {
      member <- OptionT(request.forMemberOpt(identity))
      subscription <- OptionT(tp.subscriptionService.get(member))
      memberSubscriber <- OptionT(Future.successful(Subscriber.Member.unapply(Subscriber(subscription, member))))
    } yield new SubscriptionRequest[A](tp, request) with Subscriber {
      override def subscriber = memberSubscriber
    }).run
  }

  def subscriptionRefiner(onNonMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[AuthRequest, SubReqWithSub] {
    override def refine[A](request: AuthRequest[A]): SubRequestOrResult[A] = {
      getSubRequest(request).map(_ toRight onNonMember(request))
    }
  }

  def paidSubscriptionRefiner(onFreeMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[SubReqWithSub, SubReqWithPaid] {
    type PaidMemberSub = Sub with PaidPS[PaidMembershipPlan[SubStatus, PaidTier, BillingPeriod]]
    override protected def refine[A](request: SubReqWithSub[A]): Future[Either[Result, SubReqWithPaid[A]]] =
      Future.successful(request.subscriber match {
        case PaidMember(mem) => Right(new SubscriptionRequest[A](request) with PaidSubscriber {
          override def subscriber = mem
        })
        case _ => Left(onFreeMember(request))
      })
  }


  def freeSubscriptionRefiner(onPaidMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[SubReqWithSub, SubReqWithFree] {
    type FreeMemberSub = Sub with FreePS[FreeMembershipPlan[SubStatus, FreeTier]]
    override protected def refine[A](request: SubReqWithSub[A]): Future[Either[Result, SubReqWithFree[A]]] =
      Future.successful(request.subscriber match {
        case FreeMember(mem) => Right(new SubscriptionRequest[A](request) with FreeSubscriber {
          override def subscriber = mem
        })
        case _ => Left(onPaidMember(request))
      })
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
    case _ => Ok(views.html.tier.upgrade.unavailable(req.subscriber.subscription.plan.tier, selectedTier))
  }

  def onlyNonMemberFilter(onMember: SubReqWithSub[_] => Result = memberHome(_)) = new ActionFilter[AuthRequest] {
    override def filter[A](request: AuthRequest[A]) = getSubRequest(request).map(_.map(onMember))
  }

  def isInAuthorisedGroupGoogleAuthReq(includedGroups: Set[String],
                          errorWhenNotInAcceptedGroups: Html) = new ActionFilter[GoogleAuthRequest] {
    override def filter[A](request: GoogleAuthRequest[A]) =
      isInAuthorisedGroup(includedGroups, errorWhenNotInAcceptedGroups, request.user.email, request)
  }

  def isInAuthorisedGroupIdentityGoogleAuthReq(includedGroups: Set[String],
                          errorWhenNotInAcceptedGroups: Html) = new ActionFilter[IdentityGoogleAuthRequest] {
    override def filter[A](request: IdentityGoogleAuthRequest[A]) =
      isInAuthorisedGroup(includedGroups, errorWhenNotInAcceptedGroups, request.googleUser.email, request)
  }

  def isInAuthorisedGroup(includedGroups: Set[String], errorWhenNotInAcceptedGroups: Html, email: String, request: Request[_]) = {
    val googleDirectoryService = new GoogleGroupChecker(Config.googleDirectoryConfig)
    for (usersGroups <- googleDirectoryService.retrieveGroupsFor(email)) yield {
      if (includedGroups.intersect(usersGroups).nonEmpty) None else {
        logger.info(s"Excluding $email from '${request.path}' - not in accepted groups: $includedGroups")
        Some(unauthorisedStaff(errorWhenNotInAcceptedGroups)(request))
      }
    }

  }

  def googleAuthenticationRefiner(onNonAuthentication: RequestHeader => Result = OAuthActions.sendForAuth) = {
    new ActionRefiner[AuthRequest, IdentityGoogleAuthRequest] {
      override def refine[A](request: AuthRequest[A]) = Future.successful {
        //Copy the private helper method in play-googleauth to ensure the user is Google auth'd
        //see https://github.com/guardian/play-googleauth/blob/master/module/src/main/scala/com/gu/googleauth/actions.scala#L59-60
        val userIdentityOpt = googleAuthUserOpt(request).map(IdentityGoogleAuthRequest(_, request))
        userIdentityOpt.toRight(onNonAuthentication(request))
      }
    }
  }

  def googleAuthUserOpt(request: RequestHeader) = UserIdentity.fromRequest(request).filter(_.isValid || !OAuthActions.authConfig.enforceValidity)

  def matchingGuardianEmail(onNonGuEmail: RequestHeader => Result =
                            joinStaffMembership(_).flashing("error" -> "Identity email must match Guardian email")) = new ActionFilter[IdentityGoogleAuthRequest] {
    override def filter[A](request: IdentityGoogleAuthRequest[A]) = {
      for {
        user <- IdentityService(IdentityApi).getFullUserDetails(request.identityUser, IdentityRequest(request))
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
