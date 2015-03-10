package actions

import actions.Fallbacks._
import actions.Functions._
import com.gu.googleauth
import configuration.Config
import controllers._
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthenticationService
import utils.GuMemCookie

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CommonActions {

  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))

  val Cors = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      block(request).map { result =>
        (for (originHeader <- request.headers.get(ORIGIN) if Config.corsAllowOrigin.contains(originHeader)) yield {
          result.withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN -> originHeader,
            ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true")
        }).getOrElse(result)
      }
    }
  }

  val AuthenticatedAction = NoCacheAction andThen authenticated()

  val AuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter()

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest] = OAuthActions.AuthAction

  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction

  val permanentStaffGroups = Config.staffAuthorisedEmailGroups

  val PermanentStaffNonMemberAction =
    GoogleAuthenticatedStaffAction andThen
    isInAuthorisedGroupGoogleAuthReq(permanentStaffGroups, views.html.fragments.oauth.staffUnauthorisedError())

  val AuthorisedStaff =
    GoogleAuthenticatedStaffAction andThen
    isInAuthorisedGroupGoogleAuthReq(permanentStaffGroups, views.html.fragments.oauth.staffWrongGroup())

  val AuthenticatedStaffNonMemberAction =
    AuthenticatedAction andThen
    onlyNonMemberFilter() andThen
    googleAuthenticationRefiner() andThen
    isInAuthorisedGroupIdentityGoogleAuthReq(permanentStaffGroups, views.html.fragments.oauth.staffUnauthorisedError())

  val MemberAction = AuthenticatedAction andThen memberRefiner()

  val StaffMemberAction = AuthenticatedAction andThen memberRefiner(onNonMember = joinStaffMembership(_))

  val PaidMemberAction = MemberAction andThen paidMemberRefiner()

  val AjaxAuthenticatedAction = Cors andThen NoCacheAction andThen authenticated(onUnauthenticated = createBasicGuMemCookie(_))

  val AjaxMemberAction = AjaxAuthenticatedAction andThen memberRefiner(onNonMember = createBasicGuMemCookie(_))

  val AjaxPaidMemberAction = AjaxMemberAction andThen paidMemberRefiner(onFreeMember = _ => Forbidden)

  def createBasicGuMemCookie(implicit request: RequestHeader) =
    AuthenticationService.authenticatedUserFor(request).fold(Forbidden.discardingCookies(DiscardingCookie("GU_MEM"))) { user =>
      val json = Json.obj("userId" -> user.id)
      Ok(json).withCookies(Cookie("GU_MEM", GuMemCookie.encodeUserJson(json), secure = true, httpOnly = false))
    }
}

trait OAuthActions extends googleauth.Actions {
  val authConfig = Config.googleAuthConfig

  val loginTarget = routes.OAuth.loginAction()
}

object OAuthActions extends OAuthActions
