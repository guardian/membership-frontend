package controllers

import configuration.Config
import configuration.CopyConfig
import forms.MemberForm._
import model.{FlashMessage, PageInfo}
import play.api.mvc.Controller
import services.EmailService
import actions.Functions._

import scala.concurrent.Future

trait Info extends Controller {

  val permanentStaffGroups = Config.staffAuthorisedEmailGroups
  val AuthorisedStaff = GoogleAuthenticatedStaffAction andThen isInAuthorisedGroupGoogleAuthReq(
    permanentStaffGroups, views.html.fragments.oauth.staffWrongGroup())

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def about = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleAbout,
      request.path,
      Some(CopyConfig.copyDescriptionAbout)
    )
    Ok(views.html.info.about(pageInfo))
  }

  def feedback = NoCacheAction { implicit request =>
    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    Ok(views.html.info.feedback(flashMsgOpt))
  }

  def giftingPlaceholder = NoCacheAction { implicit request =>
    Ok(views.html.info.giftingPlaceholder())
  }


  // TODO move this to CachedAction once this work is ready to go into the wild
  def supporters = AuthorisedStaff { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleSupporters,
      request.path,
      Some(CopyConfig.copyDescriptionSupporters)
    )
    Ok(views.html.info.supporters(pageInfo))
  }

  def patron() = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitlePatrons,
      request.path,
      Some(CopyConfig.copyDescriptionPatrons)
    )
    Ok(views.html.info.patron(pageInfo))
  }

  def submitFeedback = NoCacheAction.async { implicit request =>
    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

  private def sendFeedback(formData: FeedbackForm) = {
    EmailService.sendFeedback(formData)

    Future.successful(Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us"))
  }
}

object Info extends Info
