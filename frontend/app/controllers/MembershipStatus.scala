package controllers

import com.gu.i18n._
import com.gu.stripe.Stripe
import configuration.Config
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc.{Controller, Cookie, Result}
import services.{AuthenticationService, TouchpointBackend}
import com.netaporter.uri.dsl._
import views.support.{TestTrait, _}

import scalaz.syntax.std.option._
import scala.concurrent.Future

object MembershipStatus extends Controller {




  // Once things have settled down and we have a reasonable idea of what might
  // and might not vary between different countries, we should merge these country-specific
  // controllers & templates into a single one which varies on a number of parameters
  def load = AuthenticatedAction { implicit request =>
    Ok(views.html.info.membershipStatus())
  }




}
