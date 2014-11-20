package actions

import actions.Functions._
import com.gu.googleauth.UserIdentity
import com.gu.{identity, googleauth}
import configuration.Config
import controllers.{routes, Cached, NoCache}
import play.api.http.HeaderNames._
import play.api.mvc.ActionBuilder
import play.api.mvc.Results._
import play.api.mvc.Security.AuthenticatedRequest

trait CommonActions {
  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))

  val Cors = resultModifier(_.withHeaders(
    ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"))

  val AuthenticatedAction = NoCacheAction andThen authenticated()

  val AuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter()

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest] = OAuthActions.AuthAction

  val AuthenticatedStaffNonMemberAction = NoCacheAction andThen GoogleAuthAction

  val MemberAction = NoCacheAction andThen authenticated() andThen memberRefiner()

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
