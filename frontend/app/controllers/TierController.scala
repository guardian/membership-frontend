package controllers

import model.Stripe.Plan
import model.Tier.Tier
import model.{Member, Tier}

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.{MemberRequest, AuthRequest, MemberAction, AuthenticatedAction}
import services.{MemberService, StripeService}
import forms.MemberForm._

trait DowngradeTier {
  self: TierController =>

  def friendDowngrade() = MemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def friendDowngradeConfirm() = MemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- MemberService.cancelPayment(request.member)
    } yield {
      cancelledSubscription.map(_ => Redirect("/tier/change/friend/summary")).getOrElse(NotFound)
    }
  }

  def friendDowngradeSummary() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.tier.downgrade.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }
}

trait UpgradeTier {
  self: TierController =>

  def upgrade(tierStr: String) = MemberAction { implicit request =>
    val tier = Tier.routeMap(tierStr)

    if (request.member.tier < tier)
      Ok(views.html.tier.upgrade.upgradeForm(tier))
    else
      NotFound
  }

  def upgradeConfirm(tierStr: String) = MemberAction.async { implicit request =>
    val tier = Tier.routeMap(tierStr)

    if (request.member.tier < tier)
      paidMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makePayment(tier))
    else
      Future.successful(NotFound)
  }

  def makePayment(tier: Tier)(formData: PaidMemberChangeForm)(implicit request: MemberRequest[_]) = {
    val futureCustomer =
      if (request.member.customerId == Member.NO_CUSTOMER_ID)
        StripeService.Customer.create(request.user.getPrimaryEmailAddress, formData.payment.token)
      else
        StripeService.Customer.read(request.member.customerId)

    val planName = tier.toString + (if (formData.payment.`type` == "annual") Plan.ANNUAL_SUFFIX else "")

    for {
      customer <- futureCustomer
      subscription <- customer.subscription.map { subscription =>
        StripeService.Subscription.update(customer.id, subscription.id, planName, formData.payment.token)
      }.getOrElse {
        StripeService.Subscription.create(customer.id, planName)
      }
    } yield {
      MemberService.put(request.member.copy(tier = tier, customerId = customer.id))
      Ok("")
    }
  }
}

trait CancelTier {
  self: TierController =>

  def cancelTier() = MemberAction { implicit request =>
    Ok(views.html.tier.cancel.confirm(request.member.tier))
  }

  def cancelTierConfirm() = MemberAction.async { implicit request =>
    for {
      cancelledSubscription <- MemberService.cancelPayment(request.member)
    } yield {
      MemberService.put(request.member.copy(cancellationRequested=true))
      Redirect("/tier/cancel/summary")
    }
  }

  def cancelTierSummary() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.tier.cancel.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }
}

trait TierController extends Controller with UpgradeTier with DowngradeTier with CancelTier {

  def change() = MemberAction { implicit request =>
    Ok(views.html.tier.change(request.member.tier))
  }
}

object TierController extends TierController
