package controllers

import actions.Fallbacks._
import actions._
import com.gu.googleauth.GoogleAuthConfig
import com.gu.i18n._
import com.gu.stripe.Stripe
import configuration.Config
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc._
import services.{AuthenticationService, TouchpointBackend}
import com.netaporter.uri.dsl._
import play.api.libs.ws.WSClient
import views.support.{TestTrait, _}

import scalaz.syntax.std.option._
import scala.concurrent.{ExecutionContext, Future}

class MembershipStatus(
  override val wsClient: WSClient,
  parser: BodyParser[AnyContent],
  override implicit val executionContext: ExecutionContext,
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions
) extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions) with Controller {

  val AuthorisedTester = GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](Set(
    "membership.dev@guardian.co.uk",
    "mobile.core@guardian.co.uk",
    "membership.team@guardian.co.uk",
    "dig.dev.web-engineers@guardian.co.uk",
    "membership.testusers@guardian.co.uk",
    "touchpoint@guardian.co.uk",
    "crm@guardian.co.uk",
    "identitydev@guardian.co.uk"
  ), unauthorisedStaff(views.html.fragments.oauth.staffWrongGroup())(_))


  // Once things have settled down and we have a reasonable idea of what might
  // and might not vary between different countries, we should merge these country-specific
  // controllers & templates into a single one which varies on a number of parameters
  def load = AuthorisedTester { implicit request =>
    Ok(views.html.info.membershipStatus())
  }




}
