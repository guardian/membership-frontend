package controllers

import com.gu.memsub.Membership
import model.FreeEventTickets
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._

trait Subscription extends Controller with MemberServiceProvider with SubscriptionServiceProvider {
  def remainingTickets() = AjaxPaidMemberAction.async { implicit request =>
    for {
      subscription <- subscriptionService.unsafeGetPaid(request.member)(Membership())
      ticketsUsedCount <- memberService.getUsageCountWithinTerm(subscription, FreeEventTickets.unitOfMeasure)
    } yield {
      Ok(Json.obj(
        "totalAllocation" -> FreeEventTickets.allowance,
        "remainingAllocation" -> ticketsUsedCount.map(FreeEventTickets.allowance - _)
      ))
    }
  }
}

object Subscription extends Subscription
