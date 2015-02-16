package controllers

import actions._
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import forms.MemberForm._
import model.Zuora.PaidPreview
import model.{FriendTierPlan, PageInfo, Zuora}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Controller, DiscardingCookie}
import services.{IdentityApi, IdentityService, MemberService}

import scala.concurrent.Future

trait DowngradeTier {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm(request.member.tier))
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- request.touchpointBackend.downgradeSubscription(request.member)
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

    val futurePaidPreviewOpt = request.member match {
      case paidMember: PaidMember =>
        val previewUpgradeSubscriptionFuture = MemberService.previewUpgradeSubscription(paidMember, request.user, tier)
        val stripeCustomerFuture = request.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId)
        for {
          preview <- previewUpgradeSubscriptionFuture
          customer <- stripeCustomerFuture
        } yield Some(PaidPreview(customer.card, preview))
      case _: FreeMember => Future.successful(None)
    }

    val subscriptionService = request.touchpointBackend.subscriptionService

    if (request.member.tier < tier) {
      val subscriptionStatusFuture = subscriptionService.getSubscriptionStatus(request.member)
      val identityDetailsFuture = IdentityService(IdentityApi).getFullUserDetails(request.user, IdentityRequest(request))
      for {
        paidPreviewOpt <- futurePaidPreviewOpt
        subscriptionStatus <- subscriptionStatusFuture
        currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.current)
        user <- identityDetailsFuture
      } yield {
        val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
        request.member match {
          case paidMember: PaidMember => Ok(views.html.tier.upgrade.paidToPaid(request.member.tier, tier, user.privateFields, pageInfo, paidPreviewOpt, currentSubscription)) //todo change to read-ony form
          case _ => Ok(views.html.tier.upgrade.freeToPaid(request.member.tier, tier, user.privateFields, pageInfo, paidPreviewOpt))
        }

      }
    }
    else
      Future.successful(NotFound)
  }

  def upgradeConfirm(tier: Tier) = MemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)

    def handleFree(freeMember: FreeMember)(form: FreeMemberChangeForm) = for {
      memberId <- MemberService.upgradeFreeSubscription(freeMember, request.user, tier, form, identityRequest)
    } yield Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(tier).url))

    def handlePaid(paidMember: PaidMember) = for {
      memberId <- MemberService.upgradePaidSubscription(paidMember, request.user, tier, identityRequest)
    } yield Redirect(routes.TierController.upgradeThankyou(tier))

    val futureResult = request.member match {
      case freeMember: FreeMember =>
        freeMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), handleFree(freeMember))

      case paidMember: PaidMember => handlePaid(paidMember)
    }

    futureResult.map(_.discardingCookies(DiscardingCookie("GU_MEM"))).recover {
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
