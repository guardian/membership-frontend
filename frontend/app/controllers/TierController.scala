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

  def downgradeToFriend() = PaidMemberAction.async { implicit request =>
    request.tierPricing.map { pricing =>
      Ok(views.html.tier.downgrade.confirm(pricing, request.member.tier))
    }
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
        val pricingF = memberRequest.tierPricing

        memberRequest.member match {
          case paidMember: PaidMember =>
            val previewUpgradeSubscriptionF = MemberService.previewUpgradeSubscription(paidMember, tier)
            val stripeCustomerF = memberRequest.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId)

            for {
              preview <- previewUpgradeSubscriptionF
              pricing <- pricingF
              customer <- stripeCustomerF
              privateFields <- identityUserFieldsF
            } yield {
              val flashMsgOpt = memberRequest.flash.get("error").map(FlashMessage.error)

              Ok(views.html.tier.upgrade.paidToPaid(pricing, memberRequest.member.tier, tier, privateFields, pageInfo, PaidPreview(customer.card, preview), subscription, flashMsgOpt)(getToken, memberRequest.request))
            }
          case _ =>
            for (privateFields <- identityUserFieldsF; pricing <- pricingF) yield {
              Ok(views.html.tier.upgrade.freeToPaid(pricing, memberRequest.member.tier, tier, privateFields, pageInfo)(getToken, memberRequest.request))
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
        freeMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), handleFree(freeMember))

      case paidMember: PaidMember =>
        paidMemberChangeForm.bindFromRequest.fold(_ => Future.successful(BadRequest), handlePaid(paidMember))
    }

    futureResult.map(_.discardingCookies(DiscardingCookie("GU_MEM"))).recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error: ResultError => Forbidden
      case error: ScalaforceError => Forbidden
    }
  }

  def upgradeThankyou(tier: Tier) = Joiner.thankyou(tier, upgrade=true)
}

trait CancelTier {
  self: TierController =>

  def cancelTier() = MemberAction.async { implicit request =>
    request.tierPricing.map { pricing =>
      Ok(views.html.tier.cancel.confirm(pricing, request.member.tier))
    }
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
  def change() = MemberAction.async { implicit request =>
    request.tierPricing.map { pricing =>
      Ok(views.html.tier.change(pricing, currentTier = request.member.tier))
    }
  }
}

object TierController extends TierController
