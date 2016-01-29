package controllers

import com.gu.i18n.Country
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.promo.Writers._
import play.api.libs.json._
import play.api.mvc.Controller
import services.TouchpointBackend

object Promotions extends Controller {
  def validatePromoCode(promoCode: PromoCode, prpId: ProductRatePlanId, country: Country) = NoCacheAction { implicit request =>
    TouchpointBackend.Normal.promoService.findPromotion(promoCode)
      .fold(NotFound(Json.obj("errorMessage" -> "Unknown or expired promo code"))){ promo =>
        val result = promo.validateFor(prpId, country)
        val body = Json.obj(
          "promotion" -> Json.toJson(promo),
          "isValid" -> result.isRight,
          "errorMessage" -> result.swap.toOption.map(_.msg)
        )
        result.fold(_ => NotAcceptable(body), _ => Ok(body))
    }
  }

}
