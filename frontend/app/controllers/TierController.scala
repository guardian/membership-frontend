package controllers

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.{FreeMember, PaidMember, Member, Tier}
import com.gu.membership.salesforce.Tier.Tier

import actions._
import forms.MemberForm._
import services.{SubscriptionService, MemberRepository, MemberService, StripeService}

trait DowngradeTier {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- MemberService.downgradeSubscription(request.member, Tier.Friend)
    } yield Redirect("/tier/change/friend/summary")
  }

  def downgradeToFriendSummary() = PaidMemberAction.async { implicit request =>
    for {
      customer <- StripeService.Customer.read(request.member.stripeCustomerId)
      subscriptionStatus <- SubscriptionService.getSubscriptionStatus(request.member.salesforceAccountId)
      currentSubscription <- SubscriptionService.getSubscriptionDetails(subscriptionStatus.current)
      futureSubscription <- SubscriptionService.getSubscriptionDetails(subscriptionStatus.future.get)
    } yield Ok(views.html.tier.downgrade.summary(customer.card, currentSubscription, futureSubscription))
  }
}

trait UpgradeTier {
  self: TierController =>

  def upgrade(tier: Tier) = MemberAction { implicit request =>
    if (request.member.tier < tier)
      Ok(views.html.tier.upgrade.upgradeForm(tier))
    else
      NotFound
  }

  def upgradeConfirm(tier: Tier) = MemberAction.async { implicit request =>
    request.member match {
      case freeMember: FreeMember =>
        paidMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), formData => {
          MemberService.upgradeSubscription(freeMember, request.user, tier, formData.payment).map(_ => Ok(""))
        })
      case _ => Future.successful(NotFound)
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
      _ <- MemberService.cancelSubscription(request.member)
    } yield {
      Redirect("/tier/cancel/summary")
    }
  }

  def cancelTierSummary() = AuthenticatedAction.async { implicit request =>
    def paymentDetailsFor(memberOpt: Option[Member]) = {
      memberOpt.collect { case paidMember: PaidMember =>
        StripeService.Customer.read(paidMember.stripeCustomerId).map(_.paymentDetails)
      }.getOrElse(Future.successful(None))
    }

    for {
      member <- MemberRepository.get(request.user.id)
      paymentDetails <- paymentDetailsFor(member)
    } yield Ok(views.html.tier.cancel.summary(paymentDetails))
  }
}

trait TierController extends Controller with UpgradeTier with DowngradeTier with CancelTier {

  def change() = MemberAction { implicit request =>
    Ok(views.html.tier.change(request.member.tier))
  }
}

object TierController extends TierController
