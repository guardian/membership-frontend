package controllers

import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import model.FreeEventTickets
import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.MemberService

import scala.concurrent.Future

trait Subscription extends Controller {
  def updateCard() = AjaxPaidMemberAction.async { implicit request =>
    updateForm.bindFromRequest
      .fold(_ => Future.successful(BadRequest), stripeToken =>
        for {
          card <- request.touchpointBackend.updateDefaultCard(request.member, stripeToken)
        } yield Ok(Json.obj("last4" -> card.last4, "cardType" -> card.`type`, "type" -> card.`type`))
      ).recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
      }
  }

  def remainingTickets() = AjaxPaidMemberAction.async { implicit request =>
    for {
      (_, subscription) <- request.touchpointBackend.subscriptionService.accountWithLatestMembershipSubscription(request.member)
      ticketsUsedCount <- request.touchpointBackend.subscriptionService.getUsageCountWithinTerm(subscription, FreeEventTickets.unitOfMeasure)
    } yield {
      Ok(Json.obj("ticketsLeft" -> ticketsUsedCount.map(FreeEventTickets.allowance - _)))
    }
  }

  def updateCardPreflight() = Cors.andThen(CachedAction) { Ok.withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "Csrf-Token") }

  private val updateForm = Form { single("stripeToken" -> nonEmptyText) }
}

object Subscription extends Subscription
