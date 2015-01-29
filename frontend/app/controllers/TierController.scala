package controllers

import scala.concurrent.Future

import play.api.mvc.{DiscardingCookie, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import com.gu.membership.salesforce._
import com.gu.membership.salesforce.Tier

import actions.MemberRequest
import forms.MemberForm._
import model.StripeSerializer._
import model._
import services._
import actions._

trait DowngradeTier {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm(request.member.tier))
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- request.touchpointBackend.downgradeSubscription(request.member, FriendTierPlan)
    } yield Redirect("/tier/change/friend/summary")
  }

  def downgradeToFriendSummary() = PaidMemberAction.async { implicit request =>
    val subscriptionService = request.touchpointBackend.subscriptionService
    for {
      subscriptionStatus <- subscriptionService.getSubscriptionStatus(request.member)
      currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.current)
      futureSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.future.get)
    } yield Ok(views.html.tier.downgrade.summary(currentSubscription, futureSubscription))
  }
}

trait UpgradeTier {
  self: TierController =>

  def upgrade(tier: Tier) = MemberAction.async { implicit request =>

    def futureCustomerOpt = request.member match {
      case paidMember: PaidMember =>
        request.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId).map(Some(_))
      case _: FreeMember => Future.successful(None)
    }

    if (request.member.tier < tier) {
      for {
        customerOpt <- futureCustomerOpt
        user <- IdentityService(IdentityApi).getFullUserDetails(request.user, IdentityRequest(request))
      } yield {
        val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
        Ok(views.html.tier.upgrade.upgradeForm(request.member.tier, tier, user.privateFields, pageInfo, customerOpt.map(_.card)))
      }
    }
    else
      Future.successful(NotFound)
  }

  def upgradeConfirm(tier: Tier) = MemberAction.async { implicit request =>
    paidMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), doUpgrade(request.member, tier))
  }

  private def doUpgrade(member: Member, tier: Tier)(formData: MemberChangeForm)(implicit request: MemberRequest[_, _]) = {
    MemberService.upgradeSubscription(member, request.user, tier, formData, IdentityRequest(request))
      .map { _ =>
        val result = formData.payment.fold(Redirect(routes.TierController.upgradeThankyou(tier))) { _ =>
          Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(tier).url))
        }

        result.discardingCookies(DiscardingCookie("GU_MEM"))
      }.recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
        case error: Zuora.ResultError => Forbidden
        case error: ScalaforceError => Forbidden
      }
  }

  def upgradeThankyou(tier: Tier) = Joiner.thankyou(tier, upgrade=true)
}

trait CancelTier {
  self: TierController =>

  def cancelTier() = MemberAction { implicit request =>
    Ok(views.html.tier.cancel.confirm(request.member.tier))
  }

  def cancelTierConfirm() = MemberAction.async { implicit request =>
    for {
      _ <- request.touchpointBackend.cancelSubscription(request.member)
    } yield {
      Redirect("/tier/cancel/summary")
    }
  }

  def cancelTierSummary() = AuthenticatedAction.async { implicit request =>
    def subscriptionDetailsFor(memberOpt: Option[Member]) = {
      memberOpt.collect { case paidMember: PaidMember =>
        request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(paidMember)
      }
    }

    for {
      memberOpt <- request.touchpointBackend.memberRepository.get(request.user.id)
      subscriptionDetails <- Future.sequence(subscriptionDetailsFor(memberOpt).toSeq)
    } yield Ok(views.html.tier.cancel.summary(subscriptionDetails.headOption))
  }
}

trait TierController extends Controller with UpgradeTier with DowngradeTier with CancelTier {

  def change() = MemberAction { implicit request =>
    Ok(views.html.tier.change(request.member.tier))
  }
}

object TierController extends TierController
