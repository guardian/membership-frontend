package controllers
import com.gu.stripe.Stripe
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

  def support = CachedAction { implicit request =>
    Ok(views.html.giraffe.support(PageInfo(
        title = "Support",
        url = request.path,
        description = Some("Support the Guardian")
    )))
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
      res.map(_ => Ok(Json.obj("success" -> true))).recover { case e: Stripe.Error => BadRequest(Json.toJson(e))}
    })
  }
}
