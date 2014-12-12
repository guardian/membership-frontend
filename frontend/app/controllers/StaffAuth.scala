package controllers

import model.Flash._
import play.api.mvc.Controller

object StaffAuth extends Controller {

  def unauthorised = GoogleAuthenticatedStaffAction { implicit request =>
    val error = ErrorMessage(request.flash.get("error"))
    Ok(views.html.staff.unauthorised(error, request.flash.get("errorTemplate"), request.user.email))
  }
}
