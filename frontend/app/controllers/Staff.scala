package controllers

import actions.{CommonActions, OAuthActions}
import com.gu.googleauth.GoogleAuthConfig
import play.api.libs.ws.WSClient
import play.api.mvc._
import services._

import scala.concurrent.ExecutionContext


class Staff(
  override val wsClient: WSClient,
  eventbriteService: EventbriteCollectiveServices,
  parser: BodyParser[AnyContent],
  executionContext: ExecutionContext,
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions,
  override protected val controllerComponents: ControllerComponents
) extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions) with BaseController {

  val guLiveEvents = eventbriteService.guardianLiveEventService
  val masterclassEvents = eventbriteService.masterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.live(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def eventOverviewMasterclasses = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.masterclasses(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventOverviewDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.eventOverview.details(request.path))
  }
}

