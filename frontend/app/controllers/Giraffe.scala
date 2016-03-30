package controllers
import com.gu.stripe.Stripe
import play.api.libs.json.{JsString, JsArray, Json}
import play.api.mvc.{Result, Controller}
import play.api.libs.concurrent.Execution.Implicits._
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import services.TouchpointBackend
import scala.concurrent.Future

object Giraffe extends Controller {

  def support = CachedAction { implicit request =>
    Ok(views.html.giraffe.support())
  }

  def pay = NoCacheAction.async { implicit request =>
    supportForm.bindFromRequest().fold[Future[Result]]({ withErrors =>
      Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    },{ f =>
      val res = TouchpointBackend.Normal.stripeService.Charge.create((f.amount * 100).toInt, f.currency, f.email, "Giraffe", f.token)
      res.map(_ => Ok(Json.obj("success" -> true))).recover { case e: Stripe.Error => BadRequest(Json.toJson(e))}
    })
  }
}
