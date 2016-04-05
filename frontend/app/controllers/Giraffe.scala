package controllers
import com.gu.stripe.Stripe
import configuration.Config
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc.{Controller, Result}
import services.{AuthenticationService, TouchpointBackend}
import views.support._

import scala.concurrent.Future

object Giraffe extends Controller {

  val social: Set[Social] = Set(
    Twitter("The Panama Papers: how the world's rich and famous hide their money offshore http://www.theguardian.com/news/series/panama-papers?CMP=twt_contribute #panamapapers"),
    Facebook("http://www.theguardian.com/news/series/panama-papers?CMP=fb_contribute")
  )

  val stripe = TouchpointBackend.Normal.giraffeStripeService
  val chargeId = "charge_id"

  def support = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/727ed45d0601dc4fe85df56f6b24140c68145c16/0_0_2200_1320/1000.jpg"),
      stripePublicKey = Some(stripe.publicKey),
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      navigation = Seq.empty,
      customSignInUrl = Some(Config.idWebAppUrl)
    )
    Ok(views.html.giraffe.support(pageInfo))
  }

  def thanks = NoCacheAction { implicit request =>
    request.session.get(chargeId).fold(
      Redirect(routes.Giraffe.support().url, SEE_OTHER)
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

  def pay = NoCacheAction.async { implicit request =>
    supportForm.bindFromRequest().fold[Future[Result]]({ withErrors =>
      Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    },{ f =>
      val metadata = Map(
        "marketing-opt-in" -> f.marketing.toString,
        "email" -> f.email,
        "name" -> f.name
      ) ++ AuthenticationService.authenticatedUserFor(request).map("idUser" -> _.user.id) ++ f.postCode.map("postcode" -> _)
      val res = stripe.Charge.create(Math.min(5000, (f.amount * 100).toInt), f.currency, f.email, "Your contribution", f.token, metadata)

      res.map { charge =>
        Ok(Json.obj("redirect" -> routes.Giraffe.thanks().url))
          .withSession(chargeId -> charge.id)
      }.recover {
        case e: Stripe.Error => BadRequest(Json.toJson(e))
      }
    })
  }
}
