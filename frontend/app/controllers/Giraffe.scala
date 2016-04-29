package controllers

import com.gu.i18n._
import com.gu.stripe.Stripe
import configuration.Config
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc.{Controller, Result}
import services.{AuthenticationService, TouchpointBackend}
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import views.support._
import scalaz.syntax.std.option._

import scala.concurrent.Future

object Giraffe extends Controller {

  val social: Set[Social] = Set(
    Twitter("The Panama Papers: how the world's rich and famous hide their money offshore http://www.theguardian.com/news/series/panama-papers?CMP=twt_contribute #panamapapers"),
    Facebook("http://www.theguardian.com/news/series/panama-papers?CMP=fb_contribute")
  )

  val stripe = TouchpointBackend.Normal.giraffeStripeService
  val identity = TouchpointBackend.Normal.identityService
  val chargeId = "charge_id"
  val maxAmount: Option[Int] = 500.some

  // Once things have settled down and we have a reasonable idea of what might
  // and might not vary between different countries, we should merge these country-specific
  // controllers & templates into a single one which varies on a number of parameters
  def contribute = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/727ed45d0601dc4fe85df56f6b24140c68145c16/0_0_2200_1320/1000.jpg"),
      stripePublicKey = Some(stripe.publicKey),
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      navigation = Seq.empty,
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    Ok(views.html.giraffe.contribute(pageInfo,maxAmount))
  }

  def contributeUSA = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/727ed45d0601dc4fe85df56f6b24140c68145c16/0_0_2200_1320/1000.jpg"),
      stripePublicKey = Some(stripe.publicKey),
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      navigation = Seq.empty,
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    Ok(views.html.giraffe.contributeUSA(pageInfo, maxAmount))
  }

  def contributeAustralia = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/727ed45d0601dc4fe85df56f6b24140c68145c16/0_0_2200_1320/1000.jpg"),
      stripePublicKey = Some(stripe.publicKey),
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      navigation = Seq.empty,
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    Ok(views.html.giraffe.contributeAustralia(pageInfo, maxAmount))
  }

  def thanks = NoCacheAction { implicit request =>
    request.session.get(chargeId).fold(
      Redirect(routes.Giraffe.contribute().url, SEE_OTHER)
    )( id =>
      Ok(views.html.giraffe.thankyou(PageInfo(
        title = "Thank you for supporting the Guardian",
        url = request.path,
        image = None,
        description = Some("Your contribution is much appreciated, and will help us to maintain our independent, investigative journalism."),
        navigation = Seq.empty
      ), id, social))
    )
  }

  def thanksUSA = NoCacheAction { implicit request =>
    request.session.get(chargeId).fold(
      Redirect(routes.Giraffe.contributeUSA().url, SEE_OTHER)
    )( id =>
      Ok(views.html.giraffe.thankyouUSA(PageInfo(
        title = "Thank you for supporting the Guardian",
        url = request.path,
        image = None,
        description = Some("Your contribution is much appreciated, and will help us to maintain our independent, investigative journalism."),
        navigation = Seq.empty
      ), id, social))
    )
  }

  def thanksAustralia = NoCacheAction { implicit request =>
    request.session.get(chargeId).fold(
      Redirect(routes.Giraffe.contributeAustralia().url, SEE_OTHER)
    )( id =>
      Ok(views.html.giraffe.thankyouAustralia(PageInfo(
        title = "Thank you for supporting the Guardian",
        url = request.path,
        image = None,
        description = Some("Your contribution is much appreciated, and will help us to maintain our independent, investigative journalism."),
        navigation = Seq.empty
      ), id, social))
    )
  }

  def pay = NoCacheAction.async { implicit request =>
    supportForm.bindFromRequest().fold[Future[Result]]({ withErrors =>
      Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    },{ f =>
      val metadata = Map(
        "marketing-opt-in" -> f.marketing.toString,
        "email" -> f.email,
        "name" -> f.name
      ) ++ AuthenticationService.authenticatedUserFor(request).map("idUser" -> _.user.id) ++ f.postCode.map("postcode" -> _)
      val res = stripe.Charge.create(maxAmount.fold((f.amount*100).toInt)(max => Math.min(max * 100, (f.amount * 100).toInt)), f.currency, f.email, "Your contribution", f.token, metadata)

      AuthenticationService.authenticatedUserFor(request).map { user =>
        identity.updateUserMarketingPreferences(IdentityRequest(request), user, f.marketing)
      }

      val redirect = f.currency match {
        case USD => routes.Giraffe.thanksUSA().url
        case AUD => routes.Giraffe.thanksAustralia().url
        case _ => routes.Giraffe.thanks().url
      }

      res.map { charge =>
        Ok(Json.obj("redirect" -> redirect))
          .withSession(chargeId -> charge.id)
      }.recover {
        case e: Stripe.Error => BadRequest(Json.toJson(e))
      }
    })
  }
}
