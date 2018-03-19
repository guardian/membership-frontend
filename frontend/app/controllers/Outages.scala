package controllers

import actions.{CommonActions, OAuthActions}
import com.gu.googleauth.GoogleAuthConfig
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.ExecutionContext

class Outages(
  override val wsClient: WSClient,
  parser: BodyParser[AnyContent],
  executionContext: ExecutionContext,
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions,
  override protected val controllerComponents: ControllerComponents
) extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions) with BaseController {

  import commonActions.CachedAction

  def maintenanceMessage = CachedAction {
    Ok(views.html.info.maintenanceMessage())
  }

  def summary = GoogleAuthenticatedStaffAction {
    Ok(views.html.staff.plannedOutages())
  }
}
