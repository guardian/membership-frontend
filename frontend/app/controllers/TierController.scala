package controllers

import actions._
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import com.gu.membership.zuora.soap.models.errors.ResultError
import com.gu.membership.zuora.soap.models.{PaidPreview, SubscriptionDetails}
import forms.MemberForm._
import model.{FlashMessage, PageInfo}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Controller, DiscardingCookie, Result}
import services.{IdentityApi, IdentityService, MemberService}
import play.api.mvc.{AnyContent, Result, Controller, DiscardingCookie}
import services.{SubscriptionService, IdentityApi, IdentityService, MemberService}
import tracking.ActivityTracking
import utils.CampaignCode.extractCampaignCode
import play.filters.csrf.CSRF.Token.getToken

import scala.concurrent.Future

trait DowngradeTier extends ActivityTracking {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm(request.member.tier))
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- request.touchpointBackend.downgradeSubscription(request.member, request.user, extractCampaignCode(request))
    } yield Redirect(routes.TierController.downgradeToFriendSummary)
  }

  def downgradeToFriendSummary() = PaidMemberAction.async { implicit request =>
    val subscriptionService = request.touchpointBackend.subscriptionService
    val currentTier = request.member.tier
    val futureTierName = "Friend"
    for {
      subscriptionStatus <- subscriptionService.getSubscriptionStatus(request.member)
      currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.currentVersion)
      futureSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.futureVersionOpt.get)
    } yield Ok(views.html.tier.downgrade.summary(currentSubscription, futureSubscription, currentTier, futureTierName))
  }
}

trait UpgradeTier {
  self: TierController =>

  def upgrade(tier: Tier) = MemberAction.async { implicit memberRequest =>

    def previewUpgrade(subscription: SubscriptionDetails): Future[Result] = {
      if (subscription.inFreePeriodOffer) Future.successful(Ok(views.html.tier.upgrade.unavailable(memberRequest.member.tier, tier)))
      else {
        val identityUserFieldsF = IdentityService(IdentityApi).getFullUserDetails(memberRequest.user, IdentityRequest(memberRequest)).map(_.privateFields)

        val pageInfo = PageInfo.default.copy(stripePublicKey = Some(memberRequest.touchpointBackend.stripeService.publicKey))

        memberRequest.member match {
          case paidMember: PaidMember =>
            val previewUpgradeSubscriptionF = MemberService.previewUpgradeSubscription(paidMember, tier)
            val stripeCustomerF = memberRequest.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId)

            for {
              preview <- previewUpgradeSubscriptionF
              customer <- stripeCustomerF
              privateFields <- identityUserFieldsF
            } yield {
              val flashMsgOpt = memberRequest.flash.get("error").map(FlashMessage.error)

              Ok(views.html.tier.upgrade.paidToPaid(memberRequest.member.tier, tier, privateFields, pageInfo, PaidPreview(customer.card, preview), subscription, flashMsgOpt)(getToken, memberRequest.request))
            }
          case _ =>
            for (privateFields <- identityUserFieldsF) yield {
              Ok(views.html.tier.upgrade.freeToPaid(memberRequest.member.tier, tier, privateFields, pageInfo)(getToken, memberRequest.request))
            }
        }
      }
    }

    def currentSubscription = {
      val subscriptionService = memberRequest.touchpointBackend.subscriptionService

      val subscriptionStatusFuture = subscriptionService.getSubscriptionStatus(memberRequest.member)
      for {
        subscriptionStatus <- subscriptionStatusFuture
        currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.currentVersion)
      } yield currentSubscription
    }


    if (memberRequest.member.tier < tier) {
      for {
        subscription <- currentSubscription
        result <- previewUpgrade(subscription)
      } yield result
    }
    else Future.successful(Ok(views.html.tier.upgrade.unavailable(memberRequest.member.tier, tier)))

  }

  def upgradeConfirm(tier: Tier) = MemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)

    def handleFree(freeMember: FreeMember)(form: FreeMemberChangeForm) = for {
      memberId <- MemberService.upgradeFreeSubscription(freeMember, tier, form, identityRequest, extractCampaignCode(request))
    } yield Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(tier).url))

    def handlePaid(paidMember: PaidMember)(form: PaidMemberChangeForm) = {
      val reauthFailedMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(tier))
          .flashing("error" ->
          s"That password does not match our records. Please try again.")
      }

      def doUpgrade: Future[Result] = {
        MemberService.upgradePaidSubscription(paidMember, tier, identityRequest, extractCampaignCode(request), form).map {
          _ => Redirect(routes.TierController.upgradeThankyou(tier))
        }
      }

      for {
        status <- IdentityService(IdentityApi).reauthUser(paidMember.email, form.password, identityRequest)
        result <- if (status == 200) doUpgrade else reauthFailedMessage
      } yield result

    }

    val futureResult = request.member match {
      case freeMember: FreeMember =>
        freeMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handleFree(freeMember))

      case paidMember: PaidMember =>
        paidMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handlePaid(paidMember))
    }

    // After upgrading, let nextgen reassert the user's payment status
    val cookiesToDiscard = List(DiscardingCookie("GU_MEM"), DiscardingCookie("gu_paying_member"))

    futureResult.map(_.discardingCookies(cookiesToDiscard:_*)).recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error: ResultError => Forbidden
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
      _ <- request.touchpointBackend.cancelSubscription(request.member, request.user, extractCampaignCode(request))
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
    } yield {
      val currentTierOpt = memberOpt.map(_.tier)
      Ok(views.html.tier.cancel.summary(subscriptionDetails.headOption, currentTierOpt))
    }
  }
}

trait TierController extends Controller with UpgradeTier with DowngradeTier with CancelTier {
  def change() = MemberAction { implicit request =>
    val currentTier = request.member.tier
    val availableTiers = Tier.allPublic.filter(_ != currentTier)
    Ok(views.html.tier.change(currentTier, availableTiers))
  }
}

object TierController extends TierController
