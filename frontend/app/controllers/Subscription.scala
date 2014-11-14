package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json

import model.Stripe
import model.StripeSerializer._
import services.MemberService

trait Subscription extends Controller {
  def updateCard() = AjaxPaidMemberAction.async { implicit request =>
    updateForm.bindFromRequest
      .fold(_ => Future.successful(BadRequest), stripeToken =>
        for {
          card <- request.touchpointBackend.updateDefaultCard(request.member, stripeToken)
        } yield Ok(Json.obj("last4" -> card.last4, "cardType" -> card.`type`))
      ).recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
      }
  }

  def updateCardPreflight() = Cors.andThen(CachedAction) { Ok.withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "Csrf-Token") }

  private val updateForm = Form { single("stripeToken" -> nonEmptyText) }
}

object Subscription extends Subscription
