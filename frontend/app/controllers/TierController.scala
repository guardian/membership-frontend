package controllers

import actions.BackendProvider
import com.gu.membership.{PaidMembershipPlans, MembershipCatalog}
import com.gu.memsub.promo.PromoCode
import controllers._
import services.api.MemberService.{PendingAmendError, MemberError, NoCardError}
import services.{IdentityApi, IdentityService}
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.identity.play.PrivateFields
import com.gu.memsub.Subscriber.{PaidMember, FreeMember}
import com.gu.memsub._
import com.gu.memsub.{BillingPeriod, Membership, PaymentCard, ProductFamily}
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
import forms.MemberForm._
import model.SubscriptionOps._
import model._
import org.joda.time.LocalDate
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result}
import play.filters.csrf.CSRF.Token.getToken
import tracking.ActivityTracking
import utils.{TierChangeCookies, CampaignCode}
import views.support.{CheckoutForm, CountryWithCurrency, PageInfo, PaidToPaidUpgradeSummary}
import scalaz.std.scalaFuture._
import scalaz.syntax.std.option._
import scala.concurrent.Future
import scala.language.implicitConversions
import scalaz.{OptionT, EitherT, \/}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.syntax.std.option._
import scalaz.\/
import views.support.Pricing._



object TierController extends Controller with ActivityTracking
                                         with CatalogProvider
                                         with SubscriptionServiceProvider
                                         with MemberServiceProvider
                                         with StripeServiceProvider
                                         with PaymentServiceProvider {

  def downgradeToFriend() = PaidSubscriptionAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm(request.subscriber.subscription.plan.tier, catalog))
  }

  def downgradeToFriendConfirm = PaidSubscriptionAction.async { implicit request => // POST
    for {
      cancelledSubscription <- memberService.downgradeSubscription(request.subscriber)
    } yield Redirect(routes.TierController.downgradeToFriendSummary)
  }

  def downgradeToFriendSummary = PaidSubscriptionAction { implicit request =>
    val startDate = request.subscriber.subscription.chargedThroughDate.map(_.plusDays(1)).getOrElse(LocalDate.now).toDateTimeAtCurrentTime()
    implicit val c = catalog
    Ok(views.html.tier.downgrade.summary(
      request.subscriber.subscription,
      request.subscriber.subscription.paidPlan,
      catalog.friend, startDate)).discardingCookies(TierChangeCookies.deletionCookies: _*)
  }

  def change() = SubscriptionAction { implicit request =>
    implicit val countryGroup = UK
    implicit val c = catalog
    Ok(views.html.tier.change(request.subscriber.subscription.plan.tier, catalog))
  }

  def handleErrors(memberResult: Future[MemberError \/ _])(success: => Result): Future[Result] =
    memberResult.map { res => handleResultErrors(res.map(_ => success)) }

  def handleResultErrors(memberResult: MemberError \/ Result): Result =
    memberResult.fold({
      case PendingAmendError(subName) => Ok(views.html.tier.pendingAmend())
      case err => throw err
    }, identity)


  def cancelTier() = SubscriptionAction { implicit request =>
    Ok(views.html.tier.cancel.confirm(request.subscriber.subscription.plan.tier, catalog))
  }

  def cancelTierConfirm() = SubscriptionAction.async { implicit request =>
    handleErrors(memberService.cancelSubscription(request.subscriber)) {
      if(request.subscriber.subscription.isPaid) {
        Redirect(routes.TierController.cancelFreeTierSummary())
      } else {
        Redirect(routes.TierController.cancelPaidTierSummary())
      }
    }
  }

  def cancelFreeTierSummary = AuthenticatedAction(
    Ok(views.html.tier.cancel.summaryFree())
  )

  def cancelPaidTierSummary = PaidSubscriptionAction { implicit request =>
    implicit val c = catalog
    Ok(views.html.tier.cancel.summaryPaid(request.subscriber.subscription, request.subscriber.subscription.paidPlan.tier)).discardingCookies(TierChangeCookies.deletionCookies:_*)
  }

  def upgradePreview(target: PaidTier, code: Option[PromoCode]) = PaidSubscriptionAction.async { implicit request =>
    paymentSummary(request.subscriber, catalog.findPaid(target), code)(request, catalog, IdentityRequest(request))
      .map(summary => handleResultErrors(summary.map(s => Ok(Json.toJson(s)))))
  }

  def paymentSummary(subscriber: PaidMember, targetPlans: PaidMembershipPlans[Current, PaidTier], code: Option[PromoCode])
                    (implicit b: BackendProvider, c: MembershipCatalog, r: IdentityRequest): Future[MemberError \/ PaidToPaidUpgradeSummary] = {

    val targetPlan = targetPlans.get(subscriber.subscription.plan.billingPeriod)
    val targetChoice = PaidPlanChoice(targetPlan.tier, targetPlan.billingPeriod)
    val sub = subscriber.subscription

    (for {
      card <- EitherT(paymentService.getPaymentCard(sub.accountId).map(_ \/>[MemberError] NoCardError(sub.name)))
      preview <- EitherT(memberService.previewUpgradeSubscription(subscriber, targetChoice, code))
    } yield PaidToPaidUpgradeSummary(preview, sub, targetChoice.productRatePlanId, card)).run
  }

  def upgrade(target: PaidTier) = ChangeToPaidAction(target).async { implicit request =>
    implicit val c = catalog
    implicit val r = IdentityRequest(request)
    val sub = request.subscriber.subscription
    val stripeKey = Some(stripeService.publicKey)
    val currency = sub.currency
    val targetPlans = c.findPaid(target)
    val supportedCurrencies = targetPlans.allPricing.map(_.currency).toSet
    val countriesWithCurrencies = CountryWithCurrency.whitelisted(supportedCurrencies, currency)

    val identityUserFieldsF =
      IdentityService(IdentityApi)
        .getIdentityUserView(request.user, IdentityRequest(request))
        .map(_.privateFields)

    // Preselect the country from Identity fields
    // but the currency from Zuora account
    def pageInfo(pf: PrivateFields, billingPeriod: BillingPeriod): PageInfo = {
      val selectedCountry = pf.billingCountry.orElse(pf.country).flatMap { name =>
        CountryGroup.countries.find(_.name == name)
      }
      val formI18n = CheckoutForm(selectedCountry, currency, billingPeriod)
      PageInfo(initialCheckoutForm = formI18n, stripePublicKey = stripeKey)
    }

    request.paidOrFreeSubscriber.fold({freeSubscriber =>
      identityUserFieldsF.map(privateFields =>
        Ok(views.html.tier.upgrade.freeToPaid(
          c.friend,
          targetPlans,
          countriesWithCurrencies,
          privateFields,
          pageInfo(privateFields, BillingPeriod.year)
        )(getToken, request))
      )
    }, { paidSubscriber =>
      val billingPeriod = paidSubscriber.subscription.plan.billingPeriod
      val flashError = request.flash.get("error").map(FlashMessage.error)

      (identityUserFieldsF |@| paymentSummary(paidSubscriber, targetPlans, None)) { case (fields, summary) =>
        summary.map(s => Ok(views.html.tier.upgrade.paidToPaid(s, fields, pageInfo(fields, billingPeriod), flashError)(getToken, request)))
      }.map(handleResultErrors)
    })
  }

  def upgradeConfirm(target: PaidTier) = ChangeToPaidAction(target).async { implicit request =>
    implicit val identityRequest = IdentityRequest(request)

    def handleFree(freeMember: FreeMember)(form: FreeMemberChangeForm) = {
      val upgrade = memberService.upgradeFreeSubscription(freeMember, target, form, CampaignCode.fromRequest)
      handleErrors(upgrade) {
        Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(target).url))
      }
    }

    def handlePaid(paidMember: PaidMember)(form: PaidMemberChangeForm) = {
      val reauthFailedMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(target))
          .flashing("error" ->
            s"That password does not match our records. Please try again.")
      }

      def doUpgrade(): Future[Result] = {
        val upgrade = memberService.upgradePaidSubscription(paidMember, target, form, CampaignCode.fromRequest)
        handleErrors(upgrade) {
          Redirect(routes.TierController.upgradeThankyou(target))
        }
      }

      for {
        status <- IdentityService(IdentityApi).reauthUser(paidMember.contact.email, form.password, identityRequest)
        result <- if (status == 200) doUpgrade() else reauthFailedMessage
      } yield result

    }

    // I don't like this one bit!
    val futureResult = request.subscriber match {
      case Subscriber.FreeMember(mem) => freeMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handleFree(mem))
      case Subscriber.PaidMember(mem)  => paidMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handlePaid(mem))
      case a => throw new IllegalStateException(s"Inconsistent Salesforce state, contact with id ${a.contact.salesforceContactId}")
    }

    futureResult.map(_.discardingCookies(TierChangeCookies.deletionCookies:_*)).recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error: ZuoraPartialError => Forbidden
      case error: ScalaforceError => Forbidden
    }
  }

  def upgradeThankyou(tier: PaidTier) = Joiner.thankyou(tier, upgrade=true)

}
