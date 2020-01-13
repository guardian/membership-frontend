package controllers

import actions.CommonActions
import play.api.mvc.{BaseController, ControllerComponents}
import services._

class FrontPage(eventbriteService: EventbriteCollectiveServices, touchpointBackends: TouchpointBackends, commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CachedAction

  val liveEvents = eventbriteService.guardianLiveEventService
  val masterclassEvents = eventbriteService.masterclassEventService

  def index = CachedAction { implicit request =>
    Redirect(routes.WhatsOn.list().url, request.queryString, MOVED_PERMANENTLY)
  }

  def welcome = CachedAction { implicit request =>
    Redirect(routes.WhatsOn.list().url, request.queryString, MOVED_PERMANENTLY)
  }
}
