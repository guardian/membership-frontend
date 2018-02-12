package controllers

import actions.{CommonActions, OAuthActions}
import com.gu.googleauth.GoogleAuthFilters.LOGIN_ORIGIN_KEY
import com.gu.googleauth.{GoogleAuth, GoogleAuthConfig, UserIdentity}
import configuration.Config
import model.FlashMessage
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}


class OAuth(override val wsClient: WSClient, parser: BodyParser[AnyContent], override implicit val executionContext: ExecutionContext, googleAuthConfig: GoogleAuthConfig, commonActions: CommonActions)
  extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions) with Controller {

  import commonActions.NoCacheAction

  val ANTI_FORGERY_KEY = "antiForgeryToken"
  implicit val iWsClient = wsClient

  def login = NoCacheAction { request =>
    val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
    Ok(views.html.staff.unauthorised(flashMsgOpt))
  }

  /*
   * Redirect to Google with anti forgery token (that we keep in session storage - note that flashing is NOT secure)
   */
  def loginAction = Action.async { implicit request =>
    startGoogleLogin()
  }

  /*
  User comes back from Google.
  We must ensure we have the anti forgery token from the loginAction call and pass this into a verification call which
  will return a Future[UserIdentity] if the authentication is successful. If unsuccessful then the Future will fail.

   */
  def oauth2Callback = Action.async { implicit request =>
    val session = request.session
    GoogleAuth.validatedUserIdentity(googleAuthConfig).map { identity =>
      // We store the URL a user was trying to get to in the LOGIN_ORIGIN_KEY in AuthAction
      // Redirect a user back there now if it exists
      val redirect = session.get(LOGIN_ORIGIN_KEY) match {
        case Some(url) => Redirect(url)
        case None => Redirect(routes.FrontPage.index())
      }
      // Store the JSON representation of the identity in the session - this is checked by AuthAction later
      redirect.withSession {
        session + (UserIdentity.KEY -> Json.toJson(identity).toString) - LOGIN_ORIGIN_KEY
      }
    } recover {
      case t =>
        // you might want to record login failures here - we just redirect to the login page
        redirectWithError(s"Login failure: ${t.toString}")
    }
  }

  private def redirectWithError(errorMessage: String) =
    Redirect(routes.OAuth.login())
    .flashing("error" -> errorMessage)

}
