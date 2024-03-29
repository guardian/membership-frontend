package actions

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

  import actionRefiners.paidSubscriptionRefiner
  import touchpointActionRefiners._

  object BaseCommonActions extends CommonActions(authenticationService, testUsers, parser, executionContext, actionRefiners)

  private val AuthenticatedAction = BaseCommonActions.AuthenticatedAction
  private val AjaxAuthenticatedAction = BaseCommonActions.AjaxAuthenticatedAction

  val SubscriptionAction = AuthenticatedAction andThen subscriptionRefiner()

  val AjaxSubscriptionAction = AjaxAuthenticatedAction andThen subscriptionRefiner(onNonMember = BaseCommonActions.setGuMemCookie(_))

  val AjaxPaidSubscriptionAction = AjaxSubscriptionAction andThen paidSubscriptionRefiner(onFreeMember = _ => Forbidden)

}
