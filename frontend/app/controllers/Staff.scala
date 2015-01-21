package controllers

import actions.Functions._
import configuration.Config
import play.api.mvc.Controller
import services.GuardianLiveEventService

import scala.concurrent.Future

trait Staff extends Controller {
  val permanentStaffGroups = Config.staffAuthorisedEmailGroups
  val guLiveEvents = GuardianLiveEventService

  val AuthorisedStaff = GoogleAuthenticatedStaffAction andThen isInAuthorisedGroupGoogleAuthReq(
    permanentStaffGroups, views.html.fragments.oauth.staffWrongGroup())

  def eventOverview = AuthorisedStaff { implicit request =>
     Ok(views.html.staff.eventOverview(guLiveEvents.events, guLiveEvents.eventsDraft))
  }
}

object Staff extends Staff
