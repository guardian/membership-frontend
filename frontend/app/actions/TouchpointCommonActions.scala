package actions

import actions.ActionRefiners.{freeSubscriptionRefiner, paidSubscriptionRefiner}
import actions.Fallbacks.joinStaffMembership
import com.gu.salesforce.PaidTier
import play.api.mvc.Results.Forbidden
import play.api.mvc.{ActionTransformer, Request}
import services.{AuthenticationService, TouchpointBackends}

import scala.concurrent.Future

class TouchpointCommonActions(touchpointBackendProvider: TouchpointBackends, touchpointActionRefiners: TouchpointActionRefiners) {

  import touchpointActionRefiners._

  object BaseCommonActions extends CommonActions

  private val NoCacheAction = BaseCommonActions.NoCacheAction
  private val AuthenticatedAction = BaseCommonActions.AuthenticatedAction
  private val AjaxAuthenticatedAction = BaseCommonActions.AjaxAuthenticatedAction

  val OptionallyAuthenticatedAction = NoCacheAction andThen new ActionTransformer[Request, OptionallyAuthenticatedRequest]{
    override protected def transform[A](request: Request[A]): Future[OptionallyAuthenticatedRequest[A]] = {
      val user = AuthenticationService.authenticatedUserFor(request)
      val touchpointBackend = user.fold(touchpointBackendProvider.Normal)(touchpointBackendProvider.forUser(_))
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
