package controllers

import actions._
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import forms.MemberForm._
import model.Zuora.{SubscriptionDetails, PaidPreview}
import model.{IdUser, FlashMessage, PageInfo, Zuora}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result, Controller, DiscardingCookie}
import services.{SubscriptionService, IdentityApi, IdentityService, MemberService}

import scala.concurrent.Future

trait DowngradeTier {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm(request.member.tier))
  }

  def downgradeToFriendConfirm() = PaidMemberAction.async { implicit request => // POST
    for {
      cancelledSubscription <- request.touchpointBackend.downgradeSubscription(request.member, request.user)
    } yield Redirect(routes.TierController.downgradeToFriendSummary)
  }

  def downgradeToFriendSummary() = PaidMemberAction.async { implicit request =>
    val subscriptionService = request.touchpointBackend.subscriptionService
    val currentTier = request.member.tier
    val futureTierName = "Friend"
    for {
      subscriptionStatus <- subscriptionService.getSubscriptionStatus(request.member)
      currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.current)
      futureSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.future.get)
    } yield Ok(views.html.tier.downgrade.summary(currentSubscription, futureSubscription, currentTier, futureTierName))
  }
}

trait UpgradeTier {
  self: TierController =>

  def upgrade(tier: Tier) = MemberAction.async { implicit request =>

    def previewUpgrade(subscription: SubscriptionDetails): Future[Result] = {
      if (subscription.inFreePeriodOffer) Future.successful(Ok(views.html.tier.upgrade.unavailable(request.member.tier, tier)))
      else {
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

        val identityDetailsFuture = IdentityService(IdentityApi).getFullUserDetails(request.user, IdentityRequest(request))
        for {
          paidPreviewOpt <- futurePaidPreviewOpt
          user <- identityDetailsFuture
        } yield {
          val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)

          val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
          request.member match {
            case paidMember: PaidMember => Ok(views.html.tier.upgrade.paidToPaid(request.member.tier, tier, user.privateFields, pageInfo, paidPreviewOpt, subscription, flashMsgOpt))
            case _ => Ok(views.html.tier.upgrade.freeToPaid(request.member.tier, tier, user.privateFields, pageInfo, paidPreviewOpt))
          }
        }
      }
    }

    def currentSubscription = {
      val subscriptionService = request.touchpointBackend.subscriptionService

      val subscriptionStatusFuture = subscriptionService.getSubscriptionStatus(request.member)
      for {
        subscriptionStatus <- subscriptionStatusFuture
        currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.current)
      } yield currentSubscription
    }


    if (request.member.tier < tier) {
      for {
        subscription <- currentSubscription
        result <- previewUpgrade(subscription)
      } yield result
    }
    else Future.successful(Ok(views.html.tier.upgrade.unavailable(request.member.tier, tier)))

  }

  def upgradeConfirm(tier: Tier) = MemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)

    def handleFree(freeMember: FreeMember)(form: FreeMemberChangeForm) = for {
      memberId <- MemberService.upgradeFreeSubscription(freeMember, request.user, tier, form, identityRequest)
    } yield Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(tier).url))

    def handlePaid(paidMember: PaidMember)(form: PaidMemberChangeForm) = {
      val reauthFailedMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(tier))
          .flashing("error" ->
          s"That password does not match our records. Please try again.")
      }

      def doUpgrade: Future[Result] = {
        MemberService.upgradePaidSubscription(paidMember, request.user, tier, identityRequest).map {
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
      _ <- request.touchpointBackend.cancelSubscription(request.member, request.user)
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
    Ok(views.html.tier.change(request.member.tier))
  }
}

object TierController extends TierController
