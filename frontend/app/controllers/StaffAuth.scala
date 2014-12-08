package controllers

import play.api.mvc.Controller

object StaffAuth extends Controller {

  def unauthorised = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.staff.unauthorised(request.flash.get("error"), request.flash.get("errorTemplate"), request.user.email))
  }
}
