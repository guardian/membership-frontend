package controllers

import actions.OAuthActions
import play.api.libs.ws.WSClient
import play.api.mvc.Controller

class Outages(override val wsClient: WSClient) extends Controller with OAuthActions {
  def maintenanceMessage = CachedAction {
    Ok(views.html.info.maintenanceMessage())
  }

  def summary = GoogleAuthenticatedStaffAction {
    Ok(views.html.staff.plannedOutages())
  }
}
