package controllers

import play.api.mvc.Controller
import services.GuardianLiveEventService

import scala.concurrent.Future

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService

  def eventOverview = PermanentStaffNonMemberAction.async { implicit request =>
     Future.successful(Ok(views.html.staff.eventOverview(guLiveEvents.events)))
  }
}

object Staff extends Staff
