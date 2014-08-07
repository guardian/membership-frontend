package controllers

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.{FreeMember, PaidMember, Member, Tier}
import com.gu.membership.salesforce.Tier.Tier

import actions._
import forms.MemberForm._
import services.{MemberRepository, MemberService, StripeService}

trait DowngradeTier {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- MemberService.cancelAnySubscriptionPayment(request.member)
    } yield {
      cancelledSubscription.map(_ => Redirect("/tier/change/friend/summary")).getOrElse(NotFound)
    }
  }

  def downgradeToFriendSummary() = PaidMemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.stripeCustomerId).map { customer =>
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.tier.downgrade.summary(paymentDetails))

      response.getOrElse(NotFound)
    }
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
    if (request.member.tier < tier)
      paidMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makePayment(tier))
    else
      Future.successful(NotFound)
  }

  def makePayment(tier: Tier)(formData: PaidMemberChangeForm)(implicit request: MemberRequest[_]) = {
    request.member match {
      case freeMember: FreeMember =>
        MemberService.upgradeSubscription(freeMember, formData.payment.token, tier).map(_ => Ok(""))

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
      cancelledSubscription <- MemberService.cancelAnySubscriptionPayment(request.member)
    } yield {
      val newTier = if (request.member.tier == Tier.Friend) Tier.None else request.member.tier
      // TODO: move into MemberService
      MemberRepository.upsert(
        request.member.identityId,
        Map(
          Member.Keys.TIER -> newTier.toString,
          Member.Keys.OPT_IN -> false
        )
      )
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
