package controllers
import com.gu.stripe.Stripe
import model.{ResponsiveImageGenerator, ResponsiveImageGroup}
import play.api.libs.json.{JsString, JsArray, Json}
import play.api.mvc.{Result, Controller}
import play.api.libs.concurrent.Execution.Implicits._
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import services.TouchpointBackend
import views.support.PageInfo
import scala.concurrent.Future


object Giraffe extends Controller {

  def support = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      title = "Support",
      url = request.path,
      description = Some("Support the Guardian")
    )
    val img = ResponsiveImageGroup(
      name = Some("intro"),
      altText = Some("Patrons of the Guardian"),
      availableImages = ResponsiveImageGenerator(
        id = "8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
        sizes = List(1000, 500)
      )
    )

    Ok(views.html.giraffe.support(pageInfo, img))
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
