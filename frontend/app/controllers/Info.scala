package controllers

import actions.{AuthenticatedAction, AuthRequest}
import forms.MemberForm._
import play.api.mvc.{Action, Controller}
import services.{EmailService, MemberService}

import scala.concurrent.Future

trait Info extends Controller {

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def feedback = CachedAction { implicit request =>
    Ok(views.html.info.feedback())
  }

  def feedbackThankyou() = CachedAction { implicit request =>
    Ok(views.html.info.feedbackThankyou())
  }

  def submitFeedback = AuthenticatedAction.async { implicit request =>
    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

  private def sendFeedback(formData: FeedbackForm)(implicit request: AuthRequest[_]) = {
    EmailService.sendFeedback(formData)

    Future.successful(Redirect(routes.Info.feedbackThankyou()))
  }
}

object Info extends Info
