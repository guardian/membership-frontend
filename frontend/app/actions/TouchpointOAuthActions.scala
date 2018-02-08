package actions

import actions.Fallbacks.unauthorisedStaff
import controllers.AuthenticatedAction
import play.api.libs.ws.WSClient
import services.TouchpointBackends

import scala.concurrent.ExecutionContext

class TouchpointOAuthActions(touchpointBackendProvider: TouchpointBackends, touchpointActionRefiners: TouchpointActionRefiners, implicit private val ec: ExecutionContext, val wsClient: WSClient) extends OAuthActions {

  import touchpointActionRefiners._

  def AuthenticatedStaffNonMemberAction =
    AuthenticatedAction andThen
      onlyNonMemberFilter() andThen
      googleAuthenticationRefiner andThen
      requireGroup[IdentityGoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))
}
