package controllers


import actions.RichAuthRequest
import com.gu.i18n.Country
import com.gu.i18n.CountryGroup._
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Promotion.AnyPromotion
import com.gu.memsub.promo.Formatters._
import com.gu.memsub.promo.{InvalidProductRatePlan, _}
import com.gu.memsub.{Month, Year}
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import com.netaporter.uri.dsl._
import model._
import play.api.libs.json._
import play.api.mvc.{Controller, Result}
import services.PromoSessionService._
import services.TouchpointBackend
import views.support.PageInfo

import scalaz.syntax.std.option._
import scalaz.{Monad, \/}

object Promotions extends Controller {

  import TouchpointBackend.Normal.promoService
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
    ),    ResponsiveImageGroup(
      name=Some("kingsplace"),
      metadata=None,
      availableImages=ResponsiveImageGenerator(
        id="8bd255e0063f8c089ce9dd2124fcb4e3ff242395/0_68_1020_612",
        sizes=List(1000,500)
      )
    ),
    ResponsiveImageGroup(
      name=Some("stonehenge"),
      metadata=None,
      availableImages=ResponsiveImageGenerator(
        id="628e2a9c2ff25d1bdc6ead32d0d4407f0efcfd25/0_1048_2773_1663",
        sizes=List(800,500)
      ) ++ ResponsiveImageGenerator(
        id="628e2a9c2ff25d1bdc6ead32d0d4407f0efcfd25/0_693_2773_1662",
        sizes=List(2773,2000,1000)
      )
    )
  )

  def promotionPage(promoCodeStr: String) = CachedAction { implicit request =>

    def findTemplateForPromotion(promoCode: PromoCode, promotion: AnyPromotion, url: String) =
      promotion.promotionType match {
        case i: Incentive =>
          implicit val countryGroup = UK

          Some(views.html.promotions.englishHeritageOffer(
            TouchpointBackend.Normal.catalog.partner,
            PageInfo(
              title = promotion.title,
              url = url,
              description = Some(promotion.description)
            ),
            pageImages
          ))
        case p: PercentDiscount =>
          implicit val countryGroup = UK

          Some(views.html.promotions.discountOffer(
            TouchpointBackend.Normal.catalog.partner,
            PageInfo(
              title = promotion.title,
              url = url,
              description = Some(promotion.description)
            ),
            pageImages,
            promoCode,
            promotion
          ))
        case _ => None
      }

    val promoCode = PromoCode(promoCodeStr)
    val homepageWithCampaignCode = "/" ? ("INTCMP" -> s"FROM_P_${promoCode.get}")
    val redirectToHomepage = Redirect(homepageWithCampaignCode)
    val redirectToUpperCase = Redirect("/p/" + promoCodeStr.toUpperCase)
    val redirectToHomepageWithSession = Redirect(homepageWithCampaignCode).withCookies(sessionCookieFromCode(promoCode))

    type ResultDisjunction[A] = \/[Result,A]
    def failWhen(a:Boolean,b:Result) = Monad[ResultDisjunction].whenM(a)(\/.left(b))
    //When the bool is true, fall through the for comprehension with the result parameter (by returning it as a left disjunction)

    (
      for {
        //if we can't find the promotion, or it's invalid or un-renderable fail out of the for comprehension with a redirectToHomepage
        promotion <- promoService.findPromotion(promoCode) \/> redirectToHomepage
        _ <- failWhen(promoCodeStr.toUpperCase != promoCodeStr,redirectToUpperCase)
        _ <- failWhen(promotion.starts.isAfterNow,redirectToHomepage)
        _ <- failWhen(promotion.expires.isBeforeNow,redirectToHomepage)
        html <- findTemplateForPromotion(promoCode, promotion, request.path) \/> redirectToHomepageWithSession
        response <- \/.right(Ok(html).withCookies(sessionCookieFromCode(promoCode)))
      } yield response
     ).fold(identity, identity) //Whether or not the for comprehension succeeds, we have a result- which we want to return
  }


  def validatePromoCode(promoCode: PromoCode, tier: Tier, country: Country) = AuthenticatedAction { implicit request =>
    implicit val catalog = request.touchpointBackend.catalog

    val prpIds: Seq[ProductRatePlanId] = tier match {
      case t: PaidTier => Seq(PaidPlanChoice(t, Year()).productRatePlanId, PaidPlanChoice(t, Month()).productRatePlanId)
      case t: FreeTier => Seq(FreePlanChoice(t).productRatePlanId)
    }

    val p = for {
      promo <- request.touchpointBackend.promoService.findPromotion(promoCode).toRightDisjunction(NoSuchCode)
      results = prpIds.map(promo.validateFor(_, country))
      _ <- results.find(_.isRight).getOrElse(results.find(_.isLeft).getOrElse(\/.left(InvalidProductRatePlan)))
    } yield promo

    p.fold(
      {err => BadRequest(Json.toJson(err))},
      {promo => Ok(Json.toJson(promo))}
    )
  }
}
