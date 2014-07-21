package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json

import services.{ MemberService, StripeService }
import model.{ Stripe, Tier, Member }
import model.StripeSerializer._
import model.StripeDeserializer.readsEvent
import actions.{MemberAction, AuthenticatedAction, AuthRequest}
import configuration.Config

trait Subscription extends Controller {
  val stripeApiWebhookSecret: String

  def subscribe = AuthenticatedAction.async { implicit request =>
    paymentForm.bindFromRequest
      .fold(_ => Future.successful(BadRequest), makePayment)
  }

  private val paymentForm =
    Form { tuple("stripeToken" -> nonEmptyText, "tier" -> nonEmptyText) }

  private def makePayment(formData: (String, String))(implicit request: AuthRequest[_]) = {
    val (stripeToken, tier) = formData
    val payment = for {
      customer <- StripeService.Customer.create(request.user.getPrimaryEmailAddress, stripeToken)
      subscription <- StripeService.Subscription.create(customer.id, tier)
      member <- MemberService.insert(request.user.id, customer.id, Tier.withName(tier))
    } yield {
      /*
      We need to return an empty string due in the OK("") rather than a NoContent to issue in reqwest ajax library.
      A pull reqwest will be added to their library to solve this issue
      */
      Ok("")
    }

    payment.recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
    }
  }

  def cancel = MemberAction.async { implicit request =>
    for {
      customer <- StripeService.Customer.read(request.member.customerId)
      subscriptionIdOpt = customer.subscription.map(_.id)
      status <- subscriptionIdOpt
        .fold(Future.successful(NotFound)) {
          StripeService.Subscription.delete(customer.id, _).map(_ => Ok)
        }
    } yield status
  }

  def update = MemberAction.async { implicit request =>
    updateForm.bindFromRequest
      .fold(_ => Future.successful(Cors(BadRequest)), stripeToken =>
        for {
          customer <- StripeService.Customer.updateCard(request.member.customerId, stripeToken)
          cardOpt = customer.card
        } yield Cors(Ok(Json.obj("last4" -> cardOpt.map(_.last4), "cardType" -> cardOpt.map(_.`type`))))
      ).recover {
        case error: Stripe.Error => Cors(Forbidden(Json.toJson(error)))
      }
  }

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
