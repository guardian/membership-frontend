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

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, EitherT, OptionT, \/, \/-}
import scalaz.std.scalaFuture._

/**
 * These ActionFunctions serve as components that can be composed to build the
 * larger, more-generally useful pipelines in 'CommonActions'.
 *
 * https://www.playframework.com/documentation/2.3.x/ScalaActionsComposition
 */

object ActionRefiners {
  type SubRequestOrResult[A] = Future[Either[Result, SubReqWithSub[A]]]
  type SubRequestWithContributorOrResult[A] = Future[Either[Result, SubReqWithContributor[A]]]
  type PaidSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with PaidSubscriber]]
  type FreeSubRequestOrResult[A] = Future[Either[Result, SubscriptionRequest[A] with FreeSubscriber]]

  type SubReqWithPaid[A] = SubscriptionRequest[A] with PaidSubscriber
  type SubReqWithFree[A] = SubscriptionRequest[A] with FreeSubscriber
  type SubReqWithSub[A] = SubscriptionRequest[A] with Subscriber
  type SubReqWithContributor[A] = SubscriptionRequest[A] with Contributor
}

class ActionRefiners(parser: BodyParser[AnyContent], executionContext: ExecutionContext) extends LazyLogging {
  import ActionRefiners._
  import model.TierOrdering.upgradeOrdering
  implicit val pf: ProductFamily = Membership

  def resultModifier(f: Result => Result) = new ActionBuilder[Request, AnyContent] {

    override def parser = ActionRefiners.this.parser

    override protected def executionContext: ExecutionContext = ActionRefiners.this.executionContext

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map(f)
  }

  def authenticated(onUnauthenticated: RequestHeader => Result = chooseRegister(_))(implicit bodyParser: BodyParser[AnyContent]): ActionBuilder[AuthRequest, AnyContent] =
    new AuthenticatedBuilder(AuthenticationService.authenticatedIdUserProvider, bodyParser, onUnauthenticated)

  val PlannedOutageProtection = new ActionFilter[Request] {

    override protected def executionContext: ExecutionContext = ActionRefiners.this.executionContext

    override def filter[A](request: Request[A]) = Future.successful(PlannedOutage.currentOutage.map(_ => maintenance(request)))
  }

  def paidSubscriptionRefiner(onFreeMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[SubReqWithSub, SubReqWithPaid] {

    override protected def executionContext: ExecutionContext = ActionRefiners.this.executionContext

    override protected def refine[A](request: SubReqWithSub[A]): Future[Either[Result, SubReqWithPaid[A]]] =
      Future.successful(request.paidOrFreeSubscriber.bimap(free => onFreeMember(request), paid =>
        new SubscriptionRequest[A](request) with PaidSubscriber {
          override def subscriber = paid
        }).toEither
      )
  }

  def freeSubscriptionRefiner(onPaidMember: RequestHeader => Result = notYetAMemberOn(_)) = new ActionRefiner[SubReqWithSub, SubReqWithFree] {

    override protected def executionContext: ExecutionContext = ActionRefiners.this.executionContext

    override protected def refine[A](request: SubReqWithSub[A]): Future[Either[Result, SubReqWithFree[A]]] =
      Future.successful(request.paidOrFreeSubscriber.bimap(free =>
        new SubscriptionRequest[A](request) with FreeSubscriber {
          override def subscriber = free
        }, _ => onPaidMember(request)).swap.toEither // we convert paid to a response, thus giving Free \/ Response
      )                                              // but we need Either[Response, Free], hence the swap and toEither
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

  def matchingGuardianEmail(identityService: IdentityService, onNonGuEmail: RequestHeader => Result =
                            joinStaffMembership(_).flashing("error" -> "Identity email must match Guardian email")) = new ActionFilter[IdentityGoogleAuthRequest] {

    override protected def executionContext: ExecutionContext = ActionRefiners.this.executionContext

    override def filter[A](request: IdentityGoogleAuthRequest[A]) = {
      for {
        user <- identityService.getFullUserDetails(request.identityUser)(IdentityRequest(request))
      } yield {
        if (GuardianDomains.emailsMatch(request.googleUser.email, user.primaryEmailAddress)) None
        else Some(onNonGuEmail(request))
      }
    }
  }

  def metricRecord(cloudWatch: CloudWatch, metricName: String) = new ActionBuilder[Request, AnyContent] {

    override def parser = ActionRefiners.this.parser

    override protected def executionContext: ExecutionContext = ActionRefiners.this.executionContext

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      Timing.record(cloudWatch, metricName) {
        block(request)
      }
  }
}

// this is helping us stack future/either/option
object OptionEither {

  type FutureEither[X] = EitherT[Future, String, X]

  def apply[A](m: Future[\/[String, Option[A]]]): OptionT[FutureEither, A] =
    OptionT[FutureEither, A](EitherT[Future, String, Option[A]](m))

  def liftEither[A](x: Future[Option[A]]): OptionT[FutureEither, A] =
    apply(x.map(\/.right))

  def liftFutureOption[A](x: \/[String, A]): OptionT[FutureEither, A] =
    apply(Future.successful(x.map[Option[A]](Some.apply)))

  def liftFutureEither[A](x: Option[A]): OptionT[FutureEither, A] =
    apply(Future.successful(\/.right(x)))

}
