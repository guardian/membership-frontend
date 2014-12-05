package actions

import actions.Fallbacks._
import actions.Functions._
import com.gu.googleauth
import configuration.Config
import controllers._
import play.api.http.HeaderNames._
import play.api.mvc.ActionBuilder
import play.api.mvc.Results._

trait CommonActions {
  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))

  val Cors = resultModifier(_.withHeaders(
    ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"))

  val AuthenticatedAction = NoCacheAction andThen authenticated()

  val AuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter()

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest] = OAuthActions.AuthAction

  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction

  val permanentStaffGroups = Config.staffAuthorisedEmailGroups
  val permanentStaffEmailErrorMessage = Config.staffUnauthorisedError

  val PermanentStaffNonMemberAction =
    GoogleAuthenticatedStaffAction andThen
    isInAuthorisedGroupGoogleAuthReq(permanentStaffGroups, permanentStaffEmailErrorMessage)


  val AuthenticatedStaffNonMemberAction =
    AuthenticatedAction andThen
    onlyNonMemberFilter() andThen
    googleAuthenticationRefiner() andThen
    isInAuthorisedGroupIdentityGoogleAuthReq(permanentStaffGroups, permanentStaffEmailErrorMessage)

  val MemberAction = AuthenticatedAction andThen memberRefiner()

  val StaffMemberAction = AuthenticatedAction andThen memberRefiner(onNonMember = joinStaffMembership(_))

  val PaidMemberAction = MemberAction andThen paidMemberRefiner()

  val AjaxAuthenticatedAction = Cors andThen NoCacheAction andThen authenticated(onUnauthenticated = _ => Forbidden)

  val AjaxAuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter(onPaidMember = _ => Forbidden)

  val AjaxMemberAction = AjaxAuthenticatedAction andThen memberRefiner(onNonMember = _ => Forbidden)

  val AjaxPaidMemberAction = AjaxMemberAction andThen paidMemberRefiner(onFreeMember = _ => Forbidden)
}


trait OAuthActions extends googleauth.Actions {
  val authConfig = Config.googleAuthConfig

  val loginTarget = routes.OAuth.loginAction()
}

object OAuthActions extends OAuthActions
