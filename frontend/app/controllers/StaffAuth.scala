package controllers

import actions.OAuthActions
import model.FlashMessage
import play.api.libs.ws.WSClient
import play.api.mvc.Controller

class StaffAuth(override val wsClient: WSClient) extends Controller with OAuthActions {

  def unauthorised = GoogleAuthenticatedStaffAction { implicit request =>
    val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
    Ok(views.html.staff.unauthorised(flashMsgOpt, request.flash.get("errorTemplate"), request.user.email))
  }
}
