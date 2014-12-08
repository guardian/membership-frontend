package controllers

import configuration.CopyConfig
import forms.MemberForm._
import model.PageInfo
import play.api.mvc.Controller
import services.EmailService

import scala.concurrent.Future

trait Info extends Controller {

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
    Ok(views.html.info.feedback())
  }

  def giftingPlaceholder = NoCacheAction { implicit request =>
    Ok(views.html.info.giftingPlaceholder())
  }

  def feedbackThankyou() = CachedAction { implicit request =>
    Ok(views.html.info.feedbackThankyou())
  }

  def patron() = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitlePatrons,
      request.path,
      Some(CopyConfig.copyDescriptionPatrons)
    )
    Ok(views.html.joiner.tier.patron(pageInfo))
  }

  def submitFeedback = NoCacheAction.async { implicit request =>
    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

  private def sendFeedback(formData: FeedbackForm) = {
    EmailService.sendFeedback(formData)

    Future.successful(Redirect(routes.Info.feedbackThankyou()))
  }
}

object Info extends Info
