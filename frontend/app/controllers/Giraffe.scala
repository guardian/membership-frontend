package controllers
import com.gu.stripe.Stripe
import model.{ResponsiveImageGenerator, ResponsiveImageGroup}
import play.api.libs.json.{JsString, JsArray, Json}
import play.api.mvc.{Result, Controller}
import play.api.libs.concurrent.Execution.Implicits._
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import services.{AuthenticationService, TouchpointBackend}
import views.support.PageInfo
import scala.concurrent.Future

object Giraffe extends Controller {

  val stripe = TouchpointBackend.Normal.stripeService
  val chargeId = "charge_id"

  val img = ResponsiveImageGroup(
    name = Some("intro"),
    altText = Some("Patrons of the Guardian"),
    availableImages = ResponsiveImageGenerator(
      id = "8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
      sizes = List(1000, 500)
    )
  )

  def support = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support",
      url = request.path,
      stripePublicKey = Some(stripe.publicKey),
      description = Some("Support the Guardian")
    )
    Ok(views.html.giraffe.support(pageInfo, img))
  }

  def thanks = NoCacheAction { implicit request =>
    request.session.get(chargeId).fold(
      Redirect(routes.Giraffe.support().url, SEE_OTHER)
    )( id =>
      Ok(s"Your charge is $id")
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
      ) ++ AuthenticationService.authenticatedUserFor(request).map("idUser" -> _.user.id)
      val res = stripe.Charge.create((f.amount * 100).toInt, f.currency, f.email, "Giraffe", f.token, metadata)

      res.map { charge =>
        Ok(Json.obj("redirect" -> routes.Giraffe.thanks().url))
          .withSession(chargeId -> charge.id)
      }.recover {
        case e: Stripe.Error => BadRequest(Json.toJson(e))
      }
    })
  }
}
