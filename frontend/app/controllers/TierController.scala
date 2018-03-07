package controllers

import _root_.services.api.MemberService._
import actions.{BackendProvider, CommonActions, TouchpointCommonActions}
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.identity.play.PrivateFields
import com.gu.memsub.Benefit.PaidMemberTier
import com.gu.memsub.BillingPeriod
import com.gu.memsub.Subscriber.{FreeMember, PaidMember}
import com.gu.memsub.subsv2.{Catalog, PaidMembershipPlans}
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
import com.gu.monitoring.SafeLogger
import forms.MemberForm._
import model._
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents, Result}
import services.{IdentityApi, IdentityService, TouchpointBackends}
import tracking.ActivityTracking
import utils.RequestCountry._
import utils.{ReferralData, TierChangeCookies}
import views.support.MembershipCompat._
import views.support.Pricing._
import views.support.{CheckoutForm, CountryWithCurrency, PageInfo, PaidToPaidUpgradeSummary}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.syntax.std.option._
import scalaz.{EitherT, \/}

class TierController(
  val joinerController: Joiner,
  val identityApi: IdentityApi,
  touchpointCommonActions: TouchpointCommonActions,
  implicit val touchpointBackends: TouchpointBackends,
  commonActions: CommonActions,
  implicit val executionContext: ExecutionContext
, override protected val controllerComponents: ControllerComponents) extends BaseController
  with ActivityTracking
  with CatalogProvider
  with SubscriptionServiceProvider
  with MemberServiceProvider
  with StripeUKMembershipServiceProvider
  with StripeAUMembershipServiceProvider
  with PayPalServiceProvider
  with PaymentServiceProvider
  with ZuoraRestServiceProvider {

  import touchpointCommonActions._
  import commonActions.AuthenticatedAction

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
    //If we can't get a cancellation reason, it's not really a problem.
    val reason = cancellationReasonFrom.bindFromRequest.value.getOrElse(CancellationReason("mma_none"))

    handleErrors(memberService.cancelSubscription(request.subscriber, reason)) {
      if (request.subscriber.subscription.plan.isPaid) {
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
    Ok(views.html.tier.cancel.summaryPaid(request.subscriber.subscription)).discardingCookies(TierChangeCookies.deletionCookies: _*)
  }

  def paymentSummary(subscriber: PaidMember, targetPlans: PaidMembershipPlans[PaidMemberTier])
    (implicit b: BackendProvider, c: Catalog, r: IdentityRequest): Future[MemberError \/ PaidToPaidUpgradeSummary] = {

    val targetPlan = targetPlans.get(subscriber.subscription.plan.charges.billingPeriod)
    val targetChoice = PaidPlanChoice(targetPlan.tier, targetPlan.charges.billingPeriod)
    val sub = subscriber.subscription

    (for {
      country <- EitherT(memberService.country(subscriber.contact).map(\/.right))
      paymentMethod <- EitherT(paymentService.getPaymentMethod(sub.accountId).map(_ \/>[MemberError] NoCardError(sub.name)))
      preview <- EitherT(memberService.previewUpgradeSubscription(subscriber, targetChoice))
    } yield PaidToPaidUpgradeSummary(preview, sub, targetChoice.productRatePlanId, paymentMethod)(c)).run
  }

  def upgrade(target: PaidTier) = ChangeToPaidAction(target).async { implicit request =>
    implicit val c = catalog
    implicit val r = IdentityRequest(request)
    val sub = request.subscriber.subscription
    val currency = sub.plan.currency
    val targetPlans = c.findPaid(target)
    val supportedCurrencies = targetPlans.allPricing.map(_.currency).toSet
    val countriesWithCurrency = CountryWithCurrency.withCurrency(currency)

    val idUserFuture =
      IdentityService(identityApi)
        .getIdentityUserView(request.user, IdentityRequest(request))

    // Preselect the country from Identity fields
    // but the currency from Zuora account
    def getPageInfo(pf: PrivateFields, billingPeriod: BillingPeriod): PageInfo = {
      val selectedCountry = pf.billingCountry.orElse(pf.country).flatMap { name =>
        CountryGroup.countries.find(_.name == name)
      }
      PageInfo(
        initialCheckoutForm = CheckoutForm(selectedCountry, currency, billingPeriod),
        payPalEnvironment = Some(payPalService.config.payPalEnvironment),
        stripeUKMembershipPublicKey = Some(stripeUKMembershipService.publicKey),
        stripeAUMembershipPublicKey = Some(stripeAUMembershipService.publicKey)
      )
    }

    request.paidOrFreeSubscriber.fold({ freeSubscriber =>
      idUserFuture.map(idUser => {
        Ok(views.html.tier.upgrade.freeToPaid(
          c.friend,
          targetPlans,
          countriesWithCurrency,
          idUser,
          getPageInfo(idUser.privateFields, BillingPeriod.Year)
        )(request))
      })
    }, { paidSubscriber =>
      val billingPeriod = paidSubscriber.subscription.plan.charges.billingPeriod
      val flashError = request.flash.get("error").map(FlashMessage.error)

      (idUserFuture |@| paymentSummary(paidSubscriber, targetPlans)) { case (idUser, summary) =>
        summary.map(s => Ok(views.html.tier.upgrade.paidToPaid(s, idUser.privateFields, getPageInfo(idUser.privateFields, billingPeriod), flashError)(request)))
      }.map(handleResultErrors(_))
    })
  }

  def upgradeConfirm(target: PaidTier) = ChangeToPaidAction(target).async { implicit request =>
    implicit val identityRequest = IdentityRequest(request)
    SafeLogger.info(s"User ${request.user.id} is attempting to upgrade from ${request.subscriber.subscription.plan.tier.name} to ${target.name}...")

    def handleFree(freeMember: FreeMember)(form: FreeMemberChangeForm) = {
      val upgrade = memberService.upgradeFreeSubscription(freeMember, target, form, ReferralData.fromRequest)
      handleErrors(upgrade) {
        SafeLogger.info(s"User ${request.user.id} successfully upgraded to ${target.name}")
        Ok(Json.obj("redirect" -> routes.TierController.upgradeThankyou(target).url))
      }
    }

    def handlePaid(paidMember: PaidMember)(form: PaidMemberChangeForm) = {
      val reauthFailedMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(target))
          .flashing("error" ->
            s"That password does not match our records. Please try again.")
      }

      val noEmailMessage: Future[Result] = Future {
        Redirect(routes.TierController.upgrade(target))
          .flashing("error" ->
            s"Your email address is not on our system, please call customer services to upgrade")
      }

      def doUpgrade(): Future[Result] = {
        val upgrade = memberService.upgradePaidSubscription(paidMember, target, form, ReferralData.fromRequest)
        handleErrors(upgrade) {
          SafeLogger.info(s"User ${request.user.id} successfully upgraded to ${target.name}")
          Redirect(routes.TierController.upgradeThankyou(target))
        }
      }

      val emailFromZuora = zuoraRestService.getAccount(request.subscriber.subscription.accountId) map { account =>
        account.toOption.flatMap(_.billToContact.email)
      }

      emailFromZuora.flatMap { maybeEmail =>
        maybeEmail.map { email =>
          for {
            reauthResult <- IdentityService(identityApi).reauthUser(email, form.password).value
            result <- reauthResult.fold(_ => reauthFailedMessage, _ => doUpgrade())
          } yield result
        }.getOrElse(noEmailMessage)
      }
    }

    val futureResult = request.paidOrFreeSubscriber.fold(
      mem => freeMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handleFree(mem)),
      mem => paidMemberChangeForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo, handlePaid(mem))
    )

    futureResult.map(_.discardingCookies(TierChangeCookies.deletionCookies: _*)).recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error: ZuoraPartialError => Forbidden
      case error: ScalaforceError => Forbidden
    }
  }

  def upgradeThankyou(tier: PaidTier) = joinerController.thankyou(tier, upgrade = true)

}
