import actions.ActionRefiners._
import actions._
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.memsub.util.Timing
import com.gu.salesforce.Tier
import com.gu.stripe.Stripe
import com.gu.zuora.soap.models.errors.PaymentGatewayError
import com.typesafe.scalalogging.LazyLogging
import controllers.{IdentityRequest, routes, logger => _, _}
import controllers.Joiner._
import forms.MemberForm._
import play.api.libs.json.Json
import play.api.mvc.{Controller, Result}
import services.{PreMembershipJoiningEventFromSessionExtractor, TouchpointBackend}
import tracking.ActivityTracking
import utils.{CampaignCode, TierChangeCookies}
import utils.TestUsers.PreSigninTestCookie
import views.support.PageInfo

import scala.concurrent.Future
import scala.util.Failure

object Contributor extends Controller with ActivityTracking
  with LazyLogging
  with CatalogProvider
  with StripeServiceProvider
  with SalesforceServiceProvider
  with SubscriptionServiceProvider
  with PromoServiceProvider
  with PaymentServiceProvider
  with MemberServiceProvider {

  def NonContributorAction = NoCacheAction andThen PlannedOutageProtection andThen authenticated() andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def enterMonthlyContributionsDetails(countryGroup: CountryGroup = UK) = NonContributorAction.async { implicit request =>

    implicit val resolution: TouchpointBackend.Resolution =
      TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

    implicit val tpBackend = resolution.backend

    implicit val backendProvider: BackendProvider = new BackendProvider {
      override def touchpointBackend = tpBackend
    }

    implicit val c = catalog

    val identityRequest = IdentityRequest(request)

    (for {
      identityUser <- identityService.getIdentityUserView(request.user, identityRequest)
    } yield {
      val plans = catalog.contributor

      val pageInfo = PageInfo(
        stripePublicKey = Some(stripeService.publicKey)
      )

      Ok(views.html.joiner.form.monthlyContribution(
        plans,
        identityUser,
        pageInfo,
        Some(countryGroup),
        resolution))
    }).andThen { case Failure(e) => logger.error(s"User ${request.user.user.id} could not enter details for paid tier supporter: ${identityRequest.trackingParameters}", e) }
  }

  def joinMonthlyContribution = AuthenticatedNonMemberAction.async { implicit request =>
    monthlyContributorForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful(BadRequest(formWithErrors.errorsAsJson))
    },
      makeContributor(Ok(Json.obj("redirect" -> routes.Joiner.thankyouContributor.url))))
  }

  private def makeContributor(onSuccess: => Result)(formData: ContributorForm)(implicit request: AuthRequest[_]) = {
    logger.info(s"User ${request.user.id} attempting to become a monthly contributor...")
    val eventId = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request.session)
    implicit val bp: BackendProvider = request
    val idRequest = IdentityRequest(request)
    val campaignCode = CampaignCode.fromRequest
    val ipCountry = request.getFastlyCountry

    Timing.record(salesforceService.metrics, "createMember") {
      memberService.createContributor(request.user, formData, idRequest, campaignCode).map {
        case (sfContactId, zuoraSubName) =>
          logger.info(s"User ${request.user.id} successfully became monthly contributor $zuoraSubName.")

          //trackRegistrationViaEvent(sfContactId, request.user, eventId, campaignCode, tier)
          onSuccess
      }.recover {
        // errors due to user's card are logged at WARN level as they are not logic errors
        case error: Stripe.Error =>
          logger.warn(s"Stripe API call returned error: \n\t$error \n\tuser=${request.user.id}")
          Forbidden(Json.toJson(error))

        case error: PaymentGatewayError =>
          handlePaymentGatewayError(error, request.user.id, "monthly contributor", idRequest.trackingParameters)

        case error =>
          //salesforceService.metrics.putFailSignUp(tier)
          logger.error(s"User ${request.user.id} could not become monthly contributor member: ${idRequest.trackingParameters}", error)
          Forbidden
      }
    }
  }

  def thankyouContributor = SubscriptionAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    val prpId = request.subscriber.subscription.plan.productRatePlanId
    implicit val idReq = IdentityRequest(request)

    for {
      country <- memberService.country(request.subscriber.contact)
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.subscriber.contact)
      promotion = request.subscriber.subscription.promoCode.flatMap(c => promoService.findPromotion(c))
      validPromotion = promotion.flatMap(_.validateFor(prpId, country).map(_ => promotion).toOption.flatten)
      destination <- request.touchpointBackend.destinationService.returnDestinationFor(request.session, request.subscriber)
      paymentMethod <- paymentService.getPaymentMethod(request.subscriber.subscription.accountId)
    } yield {

      Ok(views.html.joiner.thankyou(
        request.subscriber,
        paymentSummary,
        paymentMethod,
        destination,
        false,
        validPromotion.filterNot(_.asTracking.isDefined),
        resolution
      )).discardingCookies(TierChangeCookies.deletionCookies: _*)
    }
  }

}
