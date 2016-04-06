package controllers

import model.FlashMessage
import play.api.mvc.Controller

object StaffAuth extends Controller {

  def unauthorised = GoogleAuthenticatedStaffAction { implicit request =>
    val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
    Ok(views.html.staff.unauthorised(flashMsgOpt,
                                     request.flash.get("errorTemplate"),
                                     request.user.email))
  }
}
