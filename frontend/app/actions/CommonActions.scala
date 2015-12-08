package actions

import actions.Fallbacks._
import actions.Functions._
import com.gu.googleauth
import com.gu.membership.salesforce.PaidTier
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

  val CorsPublicCachedAction = CorsPublic andThen CachedAction

  val AjaxAuthenticatedAction = Cors andThen NoCacheAction andThen authenticated(onUnauthenticated = setGuMemCookie(_))

  val AjaxMemberAction = AjaxAuthenticatedAction andThen memberRefiner(onNonMember = setGuMemCookie(_))

  val AjaxPaidMemberAction = AjaxMemberAction andThen paidMemberRefiner(onFreeMember = _ => Forbidden)

  def setGuMemCookie(implicit request: RequestHeader) =
    AuthenticationService.authenticatedUserFor(request).fold(Forbidden.discardingCookies(GuMemCookie.deletionCookie)) { user =>
      val json = Json.obj("userId" -> user.id)
      Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
    }

  def CheckTierChangeTo(targetTier: PaidTier) = new ActionRefiner[AnyMemberTierRequest, SubscriptionRequest] {
    override protected def refine[A](request: AnyMemberTierRequest[A]): Future[Either[Result, SubscriptionRequest[A]]] =
      request.touchpointBackend.memberService
        .subscriptionUpgradableTo(request.member, targetTier).map { subscription =>
          subscription
            .map(SubscriptionRequest(_, request))
            .toRight(Ok(views.html.tier.upgrade.unavailable(request.member.tier, targetTier)))
        }
  }

  def ChangeToPaidAction(targetTier: PaidTier): ActionBuilder[SubscriptionRequest] = MemberAction andThen CheckTierChangeTo(targetTier)
}

trait OAuthActions extends googleauth.Actions {
  val authConfig = Config.googleAuthConfig

  val loginTarget = routes.OAuth.loginAction()
}

object OAuthActions extends OAuthActions
