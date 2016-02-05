package controllers

import actions.RichAuthRequest
import com.gu.i18n.Country
import com.gu.i18n.CountryGroup._
import com.gu.memsub.BillingPeriod
import com.gu.memsub.promo._
import com.gu.memsub.promo.Writers._
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import model._
import play.api.libs.json._
import play.api.mvc.Controller
import services.TouchpointBackend
import views.support.PageInfo

import scalaz.syntax.std.option._

object Promotions extends Controller {

  val pageImages = Seq(
    ResponsiveImageGroup(
      name=Some("fearless"),
      metadata=Some(Grid.Metadata(
        description = Some("The Counted: people killed by police in the United States in 2015"),
        byline = Some("The Guardian US"),
        credit = None
      )),
      availableImages=ResponsiveImageGenerator(
        id="201ae0837f996f47b75395046bdbc30aea587443/0_0_1140_684",
        sizes=List(1000,500)
      )
    )
  )

  def promotionPage(promoCodeStr: String) = GoogleAuthenticatedStaffAction { implicit request =>

    def findTemplateForPromotion(promoCode: PromoCode, promotion: Promotion, url: String) =
      promotion.landingPageTemplate match {
        case EnglishHeritageOffer =>
          implicit val countryGroup = UK

          Some(views.html.promotions.englishHeritageOffer(
            TouchpointBackend.Normal.catalog.partner,
            PageInfo(
              title = "Free English Heritage membership worth £88",
              url = url,
              description = Some("LOREM IPSUM"),
              navigation = Nav.internationalLandingPageNavigation
            ),
            pageImages
          ))
        case _ => None
      }

    val promoCode = PromoCode(promoCodeStr)

    (for {
      promotion <- TouchpointBackend.Normal.promoService.findPromotion(promoCode)
      html <- if (promotion.expires.isBeforeNow) None else findTemplateForPromotion(promoCode, promotion, request.path)
    } yield Ok(html)).getOrElse(NotFound(views.html.error404()))

  }

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
