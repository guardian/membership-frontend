package controllers

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.{ MemberAction, AuthenticatedAction }
import model.{Member, Tier}
import services.{MemberService, StripeService}

trait Joiner extends Controller {

  def tierList = CachedAction { implicit request =>
    Ok(views.html.joiner.tierList())
  }

  def friend() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.friend())
  }

  def joinFriend() = AuthenticatedAction.async { implicit request =>
    for {
      member <- MemberService.insert(request.user.id, Member.NO_CUSTOMER_ID, Tier.Friend)
    } yield Redirect(routes.Joiner.thankyouFriend())
  }

  def partner() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.partner())
  }

  def patron() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.patron())
  }

  def paymentPartner() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.payment.paymentForm(Tier.Partner, 15))
  }

  def paymentPatron() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.payment.paymentForm(Tier.Patron, 60))
  }

  def thankyouFriend() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.thankyou.friend())
  }

  def thankyouPaid(tier: String) = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.joiner.thankyou.partner(subscription, card))

      response.getOrElse(NotFound)
    }
  }
}

object Joiner extends Joiner