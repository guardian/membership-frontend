package actions

import actions.Fallbacks.joinStaffMembership
import com.gu.salesforce.PaidTier
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionTransformer, AnyContent, BodyParser, Request}
import services.{AuthenticationService, TouchpointBackends}
import utils.TestUsers

import scala.concurrent.{ExecutionContext, Future}

class TouchpointCommonActions(
  touchpointBackends: TouchpointBackends,
  touchpointActionRefiners: TouchpointActionRefiners,
  authenticationService: AuthenticationService,
  testUsers: TestUsers,
  parser: BodyParser[AnyContent],
  val executionContext: ExecutionContext,
  actionRefiners: ActionRefiners
) {

  import actionRefiners.{paidSubscriptionRefiner, freeSubscriptionRefiner}
  import touchpointActionRefiners._

  object BaseCommonActions extends CommonActions(authenticationService, testUsers, parser, executionContext, actionRefiners)

  private val NoCacheAction = BaseCommonActions.NoCacheAction
  private val AuthenticatedAction = BaseCommonActions.AuthenticatedAction
  private val AjaxAuthenticatedAction = BaseCommonActions.AjaxAuthenticatedAction

  val OptionallyAuthenticatedAction = NoCacheAction andThen new ActionTransformer[Request, OptionallyAuthenticatedRequest]{

    override def executionContext = TouchpointCommonActions.this.executionContext

    override protected def transform[A](request: Request[A]): Future[OptionallyAuthenticatedRequest[A]] = {
      val user = authenticationService.authenticatedUserFor(request)
      val touchpointBackend = user.fold(touchpointBackends.Normal)(u => touchpointBackends.forUser(u.minimalUser))
      Future.successful(OptionallyAuthenticatedRequest[A](touchpointBackend,user,request))
    }
  }

  val AuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter()

  val SubscriptionAction = AuthenticatedAction andThen subscriptionRefiner()

  val ContributorAction = AuthenticatedAction andThen contributionRefiner()

  val StaffMemberAction = AuthenticatedAction andThen subscriptionRefiner(onNonMember = joinStaffMembership(_))

  val PaidSubscriptionAction = SubscriptionAction andThen paidSubscriptionRefiner()

  val FreeSubscriptionAction = SubscriptionAction andThen freeSubscriptionRefiner()

  val AjaxSubscriptionAction = AjaxAuthenticatedAction andThen subscriptionRefiner(onNonMember = BaseCommonActions.setGuMemCookie(_))

  val AjaxPaidSubscriptionAction = AjaxSubscriptionAction andThen paidSubscriptionRefiner(onFreeMember = _ => Forbidden)

  def ChangeToPaidAction(targetTier: PaidTier) = SubscriptionAction andThen checkTierChangeTo(targetTier)
}
