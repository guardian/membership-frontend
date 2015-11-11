package controllers

import actions._
import com.gu.identity.play.PrivateFields
import com.gu.membership.model.{Currency, GBP}
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
import play.filters.csrf.CSRF.Token.getToken
import services._
import tracking.ActivityTracking
import utils.CampaignCode.extractCampaignCode
import views.support.DisplayText._

import scala.concurrent.Future

trait DowngradeTier extends ActivityTracking {
  self: TierController =>

  def downgradeToFriend() = PaidMemberAction.async { implicit request =>
    for {
      cat <- request.catalog
      subsDetails <- request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(request.member)
    } yield {
      Ok(views.html.tier.downgrade.confirm(cat.unsafePaidTierPlanDetails(subsDetails).plan.tier, cat))
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

  def upgrade(tier: PaidTier) = MemberAction.async { implicit request =>
    import model.TierOrdering.upgradeOrdering
    val tp = request.touchpointBackend

    def previewUpgrade(subscription: SubscriptionDetails): Future[Result] = {
      if (subscription.inFreePeriodOffer) Future.successful(Ok(views.html.tier.upgrade.unavailable(request.member.tier, tier)))
      else {
        val identityUserFieldsF = IdentityService(IdentityApi).getFullUserDetails(request.user, IdentityRequest(request)).map(_.privateFields.getOrElse(PrivateFields()))
        val catalog = request.catalog
        val pageInfo = PageInfo.default.copy(stripePublicKey = Some(tp.stripeService.publicKey))

        request.member match {
          case Contact(d, c@PaidTierMember(_, _), p: StripePayment) =>
            val contact = Contact(d, c, p)
            val stripeCustomerF = tp.stripeService.Customer.read(contact.stripeCustomerId)
            val subscriptionDetails = tp.subscriptionService.getCurrentSubscriptionDetails(request.member)

            for {
              subs <- subscriptionDetails
              preview <- MemberService.previewUpgradeSubscription(subs, contact, tier, tp)
              cat <- catalog
              customer <- stripeCustomerF
              privateFields <- identityUserFieldsF
            } yield {
              val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
              val currentPlan = cat.unsafePaidTierPlanDetails(subs)
              val targetTierDetails = cat.paidTierDetails(tier)
              val targetPlan = targetTierDetails.byBillingPeriod(currentPlan.billingPeriod)

              Ok(views.html.tier.upgrade.paidToPaid(
                currentPlan,
                targetPlan,
                targetTierDetails.tier.benefits,
                privateFields,
                pageInfo,
                PaidPreview(customer.card, preview),
                subscription,
                flashMsgOpt)(getToken, request.request, currency))
            }
          case Contact(d, c@PaidTierMember(n, _), _) =>
            throw new IllegalStateException(s"Unexpected state: member number $n has a paid tier but no payment details")
          case Contact(d, c@FreeTierMember(_), _) =>
            for {
              privateFields <- identityUserFieldsF
              cat <- catalog
            } yield {
              val currentDetails = cat.freeTierDetails(c.tier)
              val targetDetails = cat.paidTierDetails(tier)

              Ok(views.html.tier.upgrade.freeToPaid(currentDetails, targetDetails, privateFields, pageInfo)(getToken, request.request, currency))
            }
        }
      }
    }

    def currentSubscription = {
      val subscriptionService = tp.subscriptionService

      val subscriptionStatusFuture = subscriptionService.getSubscriptionStatus(request.member)
      for {
        subscriptionStatus <- subscriptionStatusFuture
        currentSubscription <- subscriptionService.getSubscriptionDetails(subscriptionStatus.currentVersion)
      } yield currentSubscription
    }


    if (request.member.memberStatus.tier < tier) {
      for {
        subscription <- currentSubscription
        result <- previewUpgrade(subscription)
      } yield result
    }
    else Future.successful(Ok(views.html.tier.upgrade.unavailable(request.member.tier, tier)))

  }

  def upgradeConfirm(tier: PaidTier) = MemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)

    def handleFree(freeMember: Contact[Member, NoPayment])(form: FreeMemberChangeForm) = for {
      memberId <- MemberService.upgradeFreeSubscription(freeMember, tier, form, identityRequest, extractCampaignCode(request))
    } yield Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(tier).url))

    def handlePaid(paidMember: Contact[Member, StripePayment])(form: PaidMemberChangeForm) = {
      val reauthFailedMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(tier))
          .flashing("error" ->
          s"That password does not match our records. Please try again.")
      }

      def doUpgrade(): Future[Result] = {
        MemberService.upgradePaidSubscription(paidMember, tier, identityRequest, extractCampaignCode(request), form).map {
          _ => Redirect(routes.TierController.upgradeThankyou(tier))
        }
      }

      for {
        status <- IdentityService(IdentityApi).reauthUser(paidMember.email, form.password, identityRequest)
        result <- if (status == 200) doUpgrade() else reauthFailedMessage
      } yield result

    }

    val futureResult = request.member match {
      case Contact(d, c, p: NoPayment) => freeMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handleFree(Contact(d, c, p)))
      case Contact(d, c, p: StripePayment) => paidMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handlePaid(Contact(d, c, p)))
    }

    // After upgrading, let nextgen reassert the user's payment status
    val cookiesToDiscard = List(DiscardingCookie("GU_MEM"), DiscardingCookie("gu_paying_member"))

    futureResult.map(_.discardingCookies(cookiesToDiscard:_*)).recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error: ResultError => Forbidden
      case error: ScalaforceError => Forbidden
    }
  }

  def upgradeThankyou(tier: PaidTier) = Joiner.thankyou(tier, upgrade=true)
}

trait CancelTier {
  self: TierController =>

  def cancelTier() = MemberAction.async { implicit request =>
    request.catalog.map { catalog =>
      Ok(views.html.tier.cancel.confirm(request.member.tier, catalog))
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
    def subscriptionDetailsFor(memberOpt: Option[Contact[Member, PaymentMethod]]) = {
      memberOpt.collect { case Contact(d, m, p: StripePayment) =>
        request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(d)
      }
    }

    for {
      memberOpt <- request.touchpointBackend.memberRepository.getMember(request.user.id)
      subscriptionDetails <- Future.sequence(subscriptionDetailsFor(memberOpt).toSeq)
    } yield {
      val currentTierOpt = memberOpt.map(_.tier)
      Ok(views.html.tier.cancel.summary(subscriptionDetails.headOption, currentTierOpt))
    }
  }
}

trait TierController extends Controller with UpgradeTier with DowngradeTier with CancelTier {
  implicit val currency: Currency = GBP

  def change() = MemberAction.async { implicit request =>
    val catalog = request.catalog
    for {
      cat <- catalog
    } yield {
      Ok(views.html.tier.change(request.member.tier, cat))
    }
  }
}

object TierController extends TierController
