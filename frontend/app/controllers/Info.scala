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

  val identityService = IdentityService(identityApi)

}
