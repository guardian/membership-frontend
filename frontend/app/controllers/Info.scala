package controllers

import actions.{ActionRefiners, CommonActions}
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.memsub.images.{Grid, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.monitoring.SafeLogger
import configuration.{CopyConfig, Links}
import forms.FeedbackForm
import model.{ContentItemOffer, FlashMessage, OrientatedImages}
import play.api.mvc.{BaseController, ControllerComponents}
import services._
import tracking.RedirectWithCampaignCodes._
import utils.ReferralData
import utils.RequestCountry._
import views.support.PageInfo

import scala.concurrent.{ExecutionContext, Future}

class Info(
  val identityApi: IdentityApi,
  authenticationService: AuthenticationService,
  guardianContentService: GuardianContentService,
  touchpointBackends: TouchpointBackends,
  commonActions: CommonActions,
  actionRefiners: ActionRefiners,
  implicit val executionContext: ExecutionContext
, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.{CachedAction, NoCacheAction, StoreAcquisitionDataAction}
  import actionRefiners.PlannedOutageProtection

  def supporterRedirect(countryGroup: Option[CountryGroup]) = (NoCacheAction andThen StoreAcquisitionDataAction) { implicit request =>
    val determinedCountryGroup = (countryGroup orElse request.getFastlyCountryCode).getOrElse(CountryGroup.RestOfTheWorld)
    Redirect(routes.Info.supporterFor(determinedCountryGroup).url, campaignCodes(request), SEE_OTHER)
  }

  def patronsRedirect = NoCacheAction { implicit request =>
    Redirect(Links.patrons, campaignCodes(request), TEMPORARY_REDIRECT)
  }

  val CachedAndOutageProtected = CachedAction andThen PlannedOutageProtection

  def supporterFor(implicit countryGroup: CountryGroup) = CachedAndOutageProtected { implicit request =>
    Redirect(s"https://support.theguardian.com/${countryGroup.id}/support", request.queryString, MOVED_PERMANENTLY)
  }

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  val identityService = IdentityService(identityApi)



  def feedback = NoCacheAction.async { implicit request =>
    val authenticatedUser = authenticationService.authenticatedUserFor(request)
    val name = authenticatedUser.flatMap(_.minimalUser.displayName)

    val identityUser = authenticatedUser.map { user => identityService.getFullUserDetails(user.minimalUser)(IdentityRequest(request)) }
    val email  = identityUser.map(_.map(u => Some(u.primaryEmailAddress))).getOrElse(Future.successful(None))


    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    email.map { email =>
      Ok(views.html.info.feedback(flashMsgOpt, request.getQueryString("page"), name, email))
    }
  }

  def submitFeedback = NoCacheAction.async { implicit request =>

    val userOpt = authenticationService.authenticatedUserFor(request).map(_.minimalUser)
    val uaOpt = request.headers.get(USER_AGENT)

    val identityUser = userOpt.map { user => identityService.getFullUserDetails(user)(IdentityRequest(request)) }
    val email  = identityUser.map(_.map(u => Some(u.primaryEmailAddress))).getOrElse(Future.successful(None))


    def sendFeedback(formData: FeedbackForm) = {
      email.map{email=>
        EmailService.sendFeedback(formData, userOpt, email, uaOpt)
        Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us")
      }

    }

    FeedbackForm.form.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

}
