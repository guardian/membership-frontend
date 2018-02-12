package controllers

import actions.TouchpointCommonActions
import model.FreeEventTickets
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.TouchpointBackends

class Subscription(touchpointCommonActions: TouchpointCommonActions, implicit val touchpointBackends: TouchpointBackends) extends Controller with MemberServiceProvider {

  import touchpointCommonActions._

  def remainingTickets() = AjaxPaidSubscriptionAction.async { implicit request =>
    memberService.getUsageCountWithinTerm(request.subscriber.subscription, FreeEventTickets.unitOfMeasure) map { ticketsUsedCount =>
      Ok(Json.obj(
        "totalAllocation" -> FreeEventTickets.allowance,
        "remainingAllocation" -> ticketsUsedCount.map(FreeEventTickets.allowance - _)
      ))
    }
  }
}
