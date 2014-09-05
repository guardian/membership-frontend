package controllers

import actions.AuthRequest
import forms.MemberForm._
import play.api.mvc.{Action, Controller}
import services.EmailService

import scala.concurrent.Future

trait Info extends Controller {

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def about = CachedAction { implicit request =>
    Ok(views.html.info.about())
  }

  def feedback = NoCacheAction { implicit request =>
    Ok(views.html.info.feedback())
  }

  def feedbackThankyou() = CachedAction { implicit request =>
    Ok(views.html.info.feedbackThankyou())
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
