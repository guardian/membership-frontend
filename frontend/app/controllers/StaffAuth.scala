package controllers

import play.api.mvc.Controller

object StaffAuth extends Controller {

  def unauthorised = GoogleAuthenticatedStaffNonMemberAction { implicit request =>
    Ok(views.html.staff.unauthorised(request.user.email, request.flash.get("error")))
  }
}
