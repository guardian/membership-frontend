package controllers
import com.gu.i18n.Country
import com.gu.memsub.BillingPeriod
import com.gu.memsub.promo.{NoSuchCode, PromoCode}
import com.gu.memsub.promo.Writers._
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import model.{FreePlanChoice, PaidPlanChoice}
import play.api.libs.json._
import play.api.mvc.Controller
import services.TouchpointBackend
import actions.RichAuthRequest
import scalaz.syntax.std.option._

object Promotions extends Controller {
  def validatePromoCode(promoCode: PromoCode, billingPeriod: BillingPeriod, tier: Tier, country: Country) = AuthenticatedAction { implicit request =>
    implicit val catalog = request.touchpointBackend.catalog
    val prpId = tier match {
      case t: PaidTier => PaidPlanChoice(t, billingPeriod).productRatePlanId
      case t: FreeTier => FreePlanChoice(t).productRatePlanId
    }

    val p = for {
      promo <- TouchpointBackend.Normal.promoService.findPromotion(promoCode).toRightDisjunction(NoSuchCode)
      _ <- promo.validateFor(prpId, country)
    } yield promo

    Ok(p.fold(Json.toJson(_), Json.toJson(_)))
  }
}
