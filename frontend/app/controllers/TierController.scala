package controllers

import services.PromoSessionService.codeFromSession
import actions.BackendProvider
import com.gu.memsub.promo.{PromoCode, Upgrades}
import _root_.services.api.MemberService._
import com.gu.i18n.CountryGroup
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.i18n.CountryGroup._
import com.gu.identity.play.PrivateFields
import com.gu.memsub.Subscriber.{FreeMember, PaidMember}
import com.gu.memsub.subsv2.{Catalog, MonthYearPlans}
import services.{IdentityApi, IdentityService}
import com.gu.memsub.{BillingPeriod, _}
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm._
import model._
import org.joda.time.LocalDate
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result}
import play.filters.csrf.CSRF.Token.getToken
import tracking.ActivityTracking
import utils.RequestCountry._
import utils.{CampaignCode, TierChangeCookies}
import views.support.Pricing._
import views.support.{CheckoutForm, CountryWithCurrency, PageInfo, PaidToPaidUpgradeSummary}
import scala.concurrent.Future
import scala.language.implicitConversions
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.syntax.std.option._
import scalaz.{EitherT, \/}
import views.support.MembershipCompat._

object TierController extends Controller with ActivityTracking
                                         with LazyLogging
                                         with CatalogProvider
                                         with SubscriptionServiceProvider
                                         with MemberServiceProvider
                                         with StripeServiceProvider
                                         with PaymentServiceProvider
                                         with PromoServiceProvider {

  def downgradeToFriend() = PaidSubscriptionAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm(request.subscriber.subscription.plan.tier, request.touchpointBackend.catalogService.unsafeCatalog))
  }

  def downgradeToFriendConfirm = PaidSubscriptionAction.async { implicit request => // POST
    for {
      cancelledSubscription <- memberService.downgradeSubscription(request.subscriber)
    } yield Redirect(routes.TierController.downgradeToFriendSummary)
  }

  def downgradeToFriendSummary = PaidSubscriptionAction { implicit request =>
    val startDate = request.subscriber.subscription.plan.chargedThrough.map(_.plusDays(1)).getOrElse(LocalDate.now).toDateTimeAtCurrentTime()
    implicit val c = catalog
    Ok(views.html.tier.downgrade.summary(
      request.subscriber.subscription,
      catalog.friend, startDate)).discardingCookies(TierChangeCookies.deletionCookies: _*)
  }

  def change() = SubscriptionAction.async { implicit request =>
    implicit val countryGroup = UK
    implicit val c = catalog
    val isFreeSubscriber = request.paidOrFreeSubscriber.isLeft
    for {
      cg <- request.getIdentityCountryGroup
    } yield {
      if (cg.exists(_ != CountryGroup.UK) && isFreeSubscriber)
        Redirect(routes.Info.supporterRedirect(cg))
      else
        Ok(views.html.tier.change(request.subscriber.subscription.plan.tier, catalog))
    }
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
      if(request.subscriber.subscription.plan.isPaid) {
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
    Ok(views.html.tier.cancel.summaryPaid(request.subscriber.subscription)).discardingCookies(TierChangeCookies.deletionCookies:_*)
  }

  def upgradePreview(target: PaidTier, code: Option[PromoCode]) = PaidSubscriptionAction.async { implicit request =>
      paymentSummary(request.subscriber, catalog.findPaid(target), code)(request, catalog, IdentityRequest(request)).map { summary => {
        Ok(summary.fold({
          case MemberPromoError(e) => Json.obj("error" -> e.msg)
          case _ => Json.obj("error" -> "Unknown error")
        }, { summary => Json.obj(
          "summary" -> Json.toJson(summary),
          "promotion" -> Json.toJson(code.flatMap(promoService.findPromotion))
        )
        }))
      }}
  }

  def paymentSummary(subscriber: PaidMember, targetPlans: MonthYearPlans[com.gu.memsub.subsv2.CatalogPlan.PaidMember], code: Option[PromoCode])
                    (implicit b: BackendProvider, c: Catalog, r: IdentityRequest): Future[MemberError \/ PaidToPaidUpgradeSummary] = {

    val targetPlan = targetPlans.get(subscriber.subscription.plan.charges.billingPeriod)
    val targetChoice = PaidPlanChoice(targetPlan.tier, targetPlan.charges.billingPeriod)
    val sub = subscriber.subscription

    (for {
      country <- EitherT(memberService.country(subscriber.contact).map(\/.right))
      promo <- EitherT(Future.successful(promoService.validateMany[Upgrades](country, targetChoice.productRatePlanId)(code).leftMap[MemberError](MemberPromoError)))
      card <- EitherT(paymentService.getPaymentCard(sub.accountId).map(_ \/>[MemberError] NoCardError(sub.name)))
      preview <- EitherT(memberService.previewUpgradeSubscription(subscriber, targetChoice, promo))
    } yield PaidToPaidUpgradeSummary(preview, sub, targetChoice.productRatePlanId, card)(c)).run
  }

  def upgrade(target: PaidTier, promoCode: Option[PromoCode]) = ChangeToPaidAction(target).async { implicit request =>
    implicit val c = catalog
    implicit val r = IdentityRequest(request)
    val sub = request.subscriber.subscription
    val stripeKey = Some(stripeService.publicKey)
    val currency = sub.plan.currency
    val targetPlans = c.findPaid(target)
    val supportedCurrencies = targetPlans.allPricing.map(_.currency).toSet
    val countriesWithCurrency = CountryWithCurrency.withCurrency(currency)

    val identityUserFieldsF =
      IdentityService(IdentityApi)
        .getIdentityUserView(request.user, IdentityRequest(request))
        .map(_.privateFields)

    // Preselect the country from Identity fields
    // but the currency from Zuora account
    def getPageInfo(pf: PrivateFields, billingPeriod: BillingPeriod): PageInfo = {
      val selectedCountry = pf.billingCountry.orElse(pf.country).flatMap { name =>
        CountryGroup.countries.find(_.name == name)
      }
      val formI18n = CheckoutForm(selectedCountry, currency, billingPeriod)
      PageInfo(initialCheckoutForm = formI18n, stripePublicKey = stripeKey)
    }

    val providedPromoCode = promoCode orElse codeFromSession

    request.paidOrFreeSubscriber.fold({freeSubscriber =>
      identityUserFieldsF.map(privateFields => {

        // is the promoCode valid for the page being rendered
        val pageInfo = getPageInfo(privateFields, BillingPeriod.year)
        val planChoice = PaidPlanChoice(target, BillingPeriod.year)
        val validPromoCode = providedPromoCode.flatMap(promoService.validate[Upgrades](_, pageInfo.initialCheckoutForm.defaultCountry.get, planChoice.productRatePlanId).toOption)
        val validPromotion = validPromoCode.flatMap(validPromo => promoService.findPromotion(validPromo.code))
        val validTrackingPromoCode = validPromotion.filter(_.asTracking.isDefined).flatMap(p => providedPromoCode)
        val validDisplayablePromoCode = validPromotion.filterNot(_.asTracking.isDefined).flatMap(p => providedPromoCode)

        Ok(views.html.tier.upgrade.freeToPaid(
          c.friend,
          targetPlans,
          countriesWithCurrency,
          privateFields,
          pageInfo,
          validTrackingPromoCode,
          validDisplayablePromoCode
        )(getToken, request))
      })
    }, { paidSubscriber =>
      val billingPeriod = paidSubscriber.subscription.plan.charges.billingPeriod
      val flashError = request.flash.get("error").map(FlashMessage.error)

      // get a valid promotion but don't apply it to the payment summary
      val validPromo = memberService.country(paidSubscriber.contact).map { country =>
        val planChoice = PaidPlanChoice(target, paidSubscriber.subscription.plan.charges.billingPeriod)
        (promoCode orElse codeFromSession).flatMap(promoService.validate[Upgrades](_, country, planChoice.productRatePlanId).toOption)
      }

      (validPromo |@| identityUserFieldsF |@| paymentSummary(paidSubscriber, targetPlans, None)) { case (vp, fields, summary) =>
        summary.map(s => Ok(views.html.tier.upgrade.paidToPaid(s, fields, getPageInfo(fields, billingPeriod), flashError, vp)(getToken, request)))
      }.map(handleResultErrors)
    })
  }

  def upgradeConfirm(target: PaidTier) = ChangeToPaidAction(target).async { implicit request =>
    implicit val identityRequest = IdentityRequest(request)
    logger.info(s"User ${request.user.id} is attempting to upgrade from ${request.subscriber.subscription.plan.tier.name} to ${target.name}...")

    def handleFree(freeMember: FreeMember)(form: FreeMemberChangeForm) = {
      val upgrade = memberService.upgradeFreeSubscription(freeMember, target, form, CampaignCode.fromRequest)
      handleErrors(upgrade) {
        logger.info(s"User ${request.user.id} successfully upgraded to ${target.name}")
        Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(target).url))
      }
    }

    def handlePaid(paidMember: PaidMember)(form: PaidMemberChangeForm) = {
      val reauthFailedMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(target,form.promoCode))
          .flashing("error" ->
            s"That password does not match our records. Please try again.")
      }

      val noEmailMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(target,form.promoCode))
          .flashing("error" ->
            s"Your email address is not on our system, please call customer services to upgrade")
      }

      def doUpgrade(): Future[Result] = {
        val upgrade = memberService.upgradePaidSubscription(paidMember, target, form, CampaignCode.fromRequest)
        handleErrors(upgrade) {
          logger.info(s"User ${request.user.id} successfully upgraded to ${target.name}")
          Redirect(routes.TierController.upgradeThankyou(target))
        }
      }

      paidMember.contact.email.map { email =>
        for {
          status <- IdentityService(IdentityApi).reauthUser(email, form.password, identityRequest)
          result <- if (status == 200) doUpgrade() else reauthFailedMessage
        } yield result
      }.getOrElse(noEmailMessage)
    }

    val futureResult = request.paidOrFreeSubscriber.fold(
      mem => freeMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handleFree(mem)),
      mem => paidMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handlePaid(mem))
    )

    futureResult.map(_.discardingCookies(TierChangeCookies.deletionCookies:_*)).recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error: ZuoraPartialError => Forbidden
      case error: ScalaforceError => Forbidden
    }
  }

  def upgradeThankyou(tier: PaidTier) = Joiner.thankyou(tier, upgrade=true)

}
