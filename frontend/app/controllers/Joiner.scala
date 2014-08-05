package controllers

import actions.{AuthenticatedAction, PaidMemberAction}
import com.gu.membership.salesforce.Tier._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import services.{MemberRepository, StripeService}

trait Joiner extends Controller {

  def tierList = CachedAction { implicit request =>
    Ok(views.html.joiner.tierList())
  }

  def enterDetails(tier: Tier) = AuthenticatedAction { implicit request =>
    tier match {
      case Friend => Ok(views.html.joiner.detail.addressForm())
      case paidTier => Ok(views.html.joiner.payment.paymentForm(paidTier))
    }
  }

  def joinFriend() = AuthenticatedAction.async { implicit request =>
    for {
      member <- MemberRepository.upsert(request.user, "", Friend)
    } yield Redirect(routes.Joiner.thankyouFriend())
  }

  def patron() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.patron())
  }

  def thankyouFriend() = AuthenticatedAction { implicit request =>
    Ok(views.html.joiner.thankyou.friend())
  }

  def thankyouPaid(tier: Tier) = PaidMemberAction.async { implicit request =>
    StripeService.Customer.read(request.stripeCustomerId).map { customer =>
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.joiner.thankyou.partner(paymentDetails))

      response.getOrElse(NotFound)
    }
  }

}

object Joiner extends Joiner
