package controllers

import model.FreeEventTickets
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import javax.inject.{Inject, Singleton}

@Singleton
class Subscription @Inject()() extends Controller with MemberServiceProvider {
  def remainingTickets() = AjaxPaidSubscriptionAction.async { implicit request =>
    memberService.getUsageCountWithinTerm(request.subscriber.subscription, FreeEventTickets.unitOfMeasure) map { ticketsUsedCount =>
      Ok(Json.obj(
        "totalAllocation" -> FreeEventTickets.allowance,
        "remainingAllocation" -> ticketsUsedCount.map(FreeEventTickets.allowance - _)
      ))
    }
  }
}
