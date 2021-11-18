package controllers

import actions.TouchpointCommonActions
import model.FreeEventTickets
import play.api.libs.json.Json
import play.api.mvc._
import services.TouchpointBackends

import scala.concurrent.ExecutionContext

class SubscriptionController(touchpointCommonActions: TouchpointCommonActions, implicit val touchpointBackends: TouchpointBackends, implicit val executionContext: ExecutionContext, override protected val controllerComponents: ControllerComponents) extends BaseController with MemberServiceProvider {

  import touchpointCommonActions._

  def remainingTickets() = AjaxPaidSubscriptionAction { implicit request =>
    Ok(Json.obj(
      "totalAllocation" -> FreeEventTickets.allowance,
      // this was broken when it was changed so checkout was allowed on EB site, so the allowance has not been enforced for a while
      "remainingAllocation" -> FreeEventTickets.allowance
    ))
  }
}

