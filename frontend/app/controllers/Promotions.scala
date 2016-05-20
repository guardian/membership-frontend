package controllers


import actions.RichAuthRequest
import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Formatters._
import com.gu.memsub.promo.Promotion.AnyPromotionWithLandingPage
import com.gu.memsub.promo.{InvalidProductRatePlan, _}
import com.gu.memsub.{Month, Year}
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import com.netaporter.uri.dsl._
import model._
import play.api.libs.json._
import play.api.mvc.{Controller, Result}
import play.twirl.api.Html
import services.PromoSessionService._
import services.TouchpointBackend
import views.support.PageInfo

import scalaz.syntax.std.option._
import scalaz.{Monad, \/}

object Promotions extends Controller {

  import TouchpointBackend.Normal.{catalog, promoService}

  private val pageImages = Seq(
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
    ),
    ResponsiveImageGroup(
      name=Some("kingsplace"),
      metadata=None,
      availableImages=ResponsiveImageGenerator(
        id="8bd255e0063f8c089ce9dd2124fcb4e3ff242395/0_68_1020_612",
        sizes=List(1000,500)
      )
    )
  )

  private def getCheapestPaidMembershipPlan(promotion: AnyPromotionWithLandingPage) = {
    // flatMap gives a type mismatch compile error so using map and flatten instead
    promotion.appliesTo.productRatePlanIds
      .map(catalog.findPaid)
      .flatten
      .toSeq
      .sortBy(paidTier => paidTier.priceGBP.amount)
      .headOption
  }

  private def getTypeOfPaidTier(paidTier: PaidTier) = {
    catalog.findPaid(paidTier)
  }

  private def getImageForPromotionLandingPage(promotion: AnyPromotionWithLandingPage) = {
    ResponsiveImageGroup(
      metadata = None,
      availableImages = promotion.landingPage.imageUrl.map(uri => ResponsiveImage(uri.toString, uri.pathParts.last.part.replace(".jpg", "").toInt)).toSeq
    )
  }

  private def getPageInfo(promotion: AnyPromotionWithLandingPage, url: String) = PageInfo(
    title = promotion.landingPage.title.getOrElse(promotion.name),
    url = url,
    description = promotion.landingPage.description orElse Some(promotion.description)
  )

  private def findTemplateForPromotion(promoCode: PromoCode, promotion: AnyPromotionWithLandingPage, url: String)() = {

    implicit val countryGroup = CountryGroup.UK

    getCheapestPaidMembershipPlan(promotion).fold(Option.empty[Html]) {
      paidMembershipPlan => {
        promotion.promotionType match {
          case i: Incentive =>
            Some(views.html.promotions.incentiveLandingPage(
              getTypeOfPaidTier(paidMembershipPlan.tier),
              getPageInfo(promotion, url),
              getImageForPromotionLandingPage(promotion),
              promoCode,
              promotion.copy(promotionType = i)
            ))
          case p: PercentDiscount =>
            val originalPrice = paidMembershipPlan.pricing.getPrice(countryGroup.currency).get
            val discountedPrice = promotion.applyDiscountToPrice(originalPrice, paidMembershipPlan.billingPeriod)

            Some(views.html.promotions.discountLandingPage(
              paidMembershipPlan,
              getTypeOfPaidTier(paidMembershipPlan.tier),
              getPageInfo(promotion, url),
              getImageForPromotionLandingPage(promotion),
              promoCode,
              promotion.copy(promotionType = p),
              originalPrice,
              discountedPrice
            ))
          case _ => Option.empty[Html]
        }
      }
    }
  }

  def promotionPage(promoCodeStr: String) = CachedAction { implicit request =>

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
        _ <- failWhen(promotion.expires.exists(_.isBeforeNow),redirectToHomepage)
        withLanding <- Promotion.withLandingPage(promotion) \/> redirectToHomepageWithSession
        html <- findTemplateForPromotion(promoCode, withLanding, request.path) \/> redirectToHomepageWithSession
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
