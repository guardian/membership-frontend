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

  private def heroImageFor(countryGroup: CountryGroup) = countryGroup match {
    case US => ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Montage of The Guardian US Headlines"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("3c21e0ba85d6d060f586d0313525bd271ed0a033/0_0_1000_486", Seq(1000, 500), "png")
    )

    case Australia => ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Montage of The Guardian Australia Headlines"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("8a2003a809b699701111f10d3a0bef3c8e2ffa03/0_0_1000_486", Seq(1000, 500), "png")
    )

    case _ => ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("7b6e7b64f194b1f85bfc0791a23b8a25b72f39ba/0_0_1300_632", Seq(1300, 500), "png")
    )
  }

  private def detailImageFor(countryGroup: CountryGroup) = countryGroup match {
    case US => ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Your Guardian Membership certificate"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("3ece34992982eff0c5afebe7fa2c04638448b543/0_0_1080_610", Seq(1080, 500))
    )

    case Australia => ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Your Guardian Membership certificate"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("71b8bebab82bdead12273ff4e299a04dccad0d20/0_0_1080_610", Seq(1080, 500), "png")
    )

    case _ => ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 500))
    )
  }

  def supporterFor(implicit countryGroup: CountryGroup) = CachedAndOutageProtected { implicit request =>
    Redirect(s"https://support.theguardian.com/${countryGroup.id}/support", request.queryString, MOVED_PERMANENTLY)
  }

  def offersAndCompetitions = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val results =
      guardianContentService.offersAndCompetitionsContent.map(ContentItemOffer).filter(item =>
        item.content.fields.flatMap(_.membershipAccess).isEmpty && !item.content.webTitle.startsWith("EXPIRED") && item.imgOpt.nonEmpty)

    Ok(views.html.info.offersAndCompetitions(touchpointBackends.Normal.catalog, results))
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
