package actions

import actions.Fallbacks.unauthorisedStaff
import com.gu.googleauth.GoogleAuthConfig
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContent, BodyParser}
import services.TouchpointBackends

import scala.concurrent.ExecutionContext

class TouchpointOAuthActions(
  touchpointBackends: TouchpointBackends,
  touchpointActionRefiners: TouchpointActionRefiners,
  implicit private val ec: ExecutionContext,
  val wsClient: WSClient,
  parser: BodyParser[AnyContent],
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions
) extends OAuthActions(parser, ec, googleAuthConfig, commonActions) {

  import commonActions.AuthenticatedAction

  import touchpointActionRefiners._

  def AuthenticatedStaffNonMemberAction =
    AuthenticatedAction andThen
      onlyNonMemberFilter() andThen
      googleAuthenticationRefiner andThen
      requireGroup[IdentityGoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))
}
