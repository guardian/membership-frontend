package actions

import actions.ActionRefiners._
import actions.Fallbacks._
import com.gu.googleauth
import com.gu.salesforce.PaidTier
import configuration.Config
import controllers._
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthenticationService
import utils.GuMemCookie
import utils.TestUsers.isTestUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CommonActions {
  import OAuthActions._

  val AddUserInfoToResponse = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      block(request).map { result =>
        (for (user <- AuthenticationService.authenticatedUserFor(request)) yield {
          result.withHeaders(
            "X-Gu-Identity-Id" -> user.id,
            "X-Gu-Membership-Test-User" -> isTestUser(user).toString)
        }).getOrElse(result)
      }
    }

  val NoCacheAction = resultModifier(NoCache(_)) andThen AddUserInfoToResponse

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

  val CorsPublic = resultModifier { result =>
    result.withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
      ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
    )
  }

  val AuthenticatedAction = NoCacheAction andThen authenticated()

  val AuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter()

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest] = AuthAction

  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction

  val permanentStaffGroups = Config.staffAuthorisedEmailGroups

  val PermanentStaffNonMemberAction =
    GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))

  val AuthorisedStaff =
    GoogleAuthenticatedStaffAction andThen
      requireGroup[GoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffWrongGroup())(_))

  val AuthenticatedStaffNonMemberAction =
    AuthenticatedAction andThen
    onlyNonMemberFilter() andThen
    googleAuthenticationRefiner() andThen
      requireGroup[IdentityGoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))

  val SubscriptionAction = AuthenticatedAction andThen subscriptionRefiner()

  val StaffMemberAction = AuthenticatedAction andThen subscriptionRefiner(onNonMember = joinStaffMembership(_))

  val PaidSubscriptionAction = SubscriptionAction andThen paidSubscriptionRefiner()

  val FreeSubscriptionAction = SubscriptionAction andThen freeSubscriptionRefiner()

  val CorsPublicCachedAction = CorsPublic andThen CachedAction

  val AjaxAuthenticatedAction = Cors andThen NoCacheAction andThen authenticated(onUnauthenticated = setGuMemCookie(_))

  val AjaxSubscriptionAction = AjaxAuthenticatedAction andThen subscriptionRefiner(onNonMember = setGuMemCookie(_))

  val AjaxPaidSubscriptionAction = AjaxSubscriptionAction andThen paidSubscriptionRefiner(onFreeMember = _ => Forbidden)

  def setGuMemCookie(implicit request: RequestHeader) =
    AuthenticationService.authenticatedUserFor(request).fold(Forbidden.discardingCookies(GuMemCookie.deletionCookie)) { user =>
      val json = Json.obj("userId" -> user.id)
      Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
    }

  def ChangeToPaidAction(targetTier: PaidTier) = SubscriptionAction andThen checkTierChangeTo(targetTier)
}

trait OAuthActions extends googleauth.Actions with googleauth.Filters {
  val authConfig = Config.googleAuthConfig

  val loginTarget = routes.OAuth.loginAction()

  lazy val groupChecker = Config.googleGroupChecker
}

object OAuthActions extends OAuthActions
