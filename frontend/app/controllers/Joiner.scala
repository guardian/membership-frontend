package controllers

import actions.{AuthenticatedAction, PaidMemberAction}
import com.gu.membership.salesforce.Tier._
import controllers.Joining._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import services.{MemberService, StripeService, EventbriteService, MemberRepository}

import scala.concurrent.Future

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

    val memberService = MemberService
    val eventService = EventbriteService
    val eventIdOpt = services.PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
    val eventOpt = eventIdOpt.map(eventService.getEvent)

    for {
      customer <- StripeService.Customer.read(request.stripeCustomerId)
      event <- Future.sequence(eventOpt.toSeq)
      discount <- memberService.createEventDiscount(request.user.id, event.headOption.get)
    } yield {
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.joiner.thankyou.partner(paymentDetails, event.headOption, discount))

      response.getOrElse(NotFound)
    }
  }

}

object Joiner extends Joiner
