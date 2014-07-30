package controllers

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.Tier

import actions.{PaidMemberAction, AuthenticatedAction}
import services.{MemberRepository, StripeService}

trait Joiner extends Controller {

  def tierList = CachedAction { implicit request =>
    Ok(views.html.joiner.tierList())
  }

  def friend() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.friend())
  }

  def detailFriend() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.detail.addressForm())
  }

  def joinFriend() = AuthenticatedAction.async { implicit request =>
    for {
      member <- MemberRepository.upsert(request.user, "", Tier.Friend)
    } yield Redirect(routes.Joiner.thankyouFriend())
  }

  def partner() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.partner())
  }

  def paymentPartner() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.payment.paymentForm(Tier.Partner, 15))
  }

  def patron() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.patron())
  }

  def paymentPatron() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.payment.paymentForm(Tier.Patron, 60))
  }

  def thankyouFriend() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.thankyou.friend())
  }

  def thankyouPaid(tier: String) = PaidMemberAction.async { implicit request =>
    StripeService.Customer.read(request.stripeCustomerId).map { customer =>
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.joiner.thankyou.partner(paymentDetails))

      response.getOrElse(NotFound)
    }
  }
}

object Joiner extends Joiner
