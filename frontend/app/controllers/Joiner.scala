package controllers

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.Tier

import actions.{AuthRequest, PaidMemberAction, AuthenticatedAction}
import services.{MemberService, StripeService}
import forms.MemberForm.{FriendJoinForm, friendJoinForm}

trait Joiner extends Controller {

  def tierList = CachedAction { implicit request =>
    Ok(views.html.joiner.tierList())
  }

  def detailFriend() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.detail.addressForm())
  }

  def joinFriend() = AuthenticatedAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makeFriend)
  }

  private def makeFriend(formData: FriendJoinForm)(implicit request: AuthRequest[_]) = {
    for {
      salesforceContactId <- MemberService.createFriend(request.user, formData)
    } yield Redirect(routes.Joiner.thankyouFriend())
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
