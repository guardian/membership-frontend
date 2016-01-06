package controllers

import model.FreeEventTickets
import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._

trait Subscription extends Controller with MemberServiceProvider {
  def remainingTickets() = AjaxPaidMemberAction.async { implicit request =>
    for {
      subscription <- memberService.currentSubscription(request.member)
      ticketsUsedCount <- memberService.getUsageCountWithinTerm(subscription, FreeEventTickets.unitOfMeasure)
    } yield {
      Ok(Json.obj(
        "totalAllocation" -> FreeEventTickets.allowance,
        "remainingAllocation" -> ticketsUsedCount.map(FreeEventTickets.allowance - _)
      ))
    }
  }

  private val updateForm = Form { single("stripeToken" -> nonEmptyText) }
}

object Subscription extends Subscription
