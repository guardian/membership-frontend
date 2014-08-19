package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json

import services.{MemberService, StripeService}
import model.Stripe
import model.StripeSerializer._
import model.StripeDeserializer.readsEvent
import actions.{Cors, PaidMemberAction, AuthenticatedAction, AuthRequest}
import configuration.Config
import forms.MemberForm._

trait Subscription extends Controller {
  val stripeApiWebhookSecret: String

  def subscribe = AuthenticatedAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makePayment)
  }

  private def makePayment(formData: PaidMemberJoinForm)(implicit request: AuthRequest[_]) = {
    val payment = for {
      salesforceContactId <- MemberService.createPaidMember(request.user, formData, IdentityHeaders(request))
    } yield Ok("")

    payment.recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
    }
  }

  def updateCard() = Cors(PaidMemberAction).async { implicit request =>
    updateForm.bindFromRequest
      .fold(_ => Future.successful(BadRequest), stripeToken =>
        for {
          card <- MemberService.updateDefaultCard(request.member, stripeToken)
        } yield Ok(Json.obj("last4" -> card.last4, "cardType" -> card.`type`))
      ).recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
      }
  }

  def updateCardPreflight() = Cors(CachedAction) { Ok.withHeaders(ACCESS_CONTROL_ALLOW_HEADERS -> "Csrf-Token") }

  private val updateForm = Form { single("stripeToken" -> nonEmptyText) }

  def event(secret: String) = NoCacheAction { implicit request =>
    if (secret == stripeApiWebhookSecret) {
      val result = for {
        json <- request.body.asJson
        event <- json.asOpt[Stripe.Event]
      } yield {
        StripeService.Events.handle(event)
        Ok
      }

      result.getOrElse(BadRequest)
    } else {
      NotFound
    }
  }
}

object Subscription extends Subscription {
  val stripeApiWebhookSecret = Config.stripeApiWebhookSecret
}
