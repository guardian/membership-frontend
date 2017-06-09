package controllers

import play.api.mvc.Controller

object Outages extends Controller {
  def maintenanceMessage = NoCacheAction {
    Ok(views.html.info.maintenanceMessage())
  }

  def summary = GoogleAuthenticatedStaffAction {
    Ok(views.html.staff.plannedOutages())
  }
}
