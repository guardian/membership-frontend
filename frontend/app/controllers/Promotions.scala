package controllers

import actions.RichAuthRequest
import com.gu.i18n.{Country, CountryGroup}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.gu.memsub.promo.Formatters.PromotionFormatters._
import com.gu.memsub.promo.Formatters._
import com.gu.memsub.promo.{InvalidProductRatePlan, _}
import com.gu.memsub.{Month, Year}
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import com.netaporter.uri.dsl._
import play.api.libs.json._
import play.api.mvc.{Controller, Result}
import play.twirl.api.Html
import services.PromoSessionService._
import services.TouchpointBackend
import views.support.PageInfo
import com.gu.memsub.promo.PercentDiscount._
import com.gu.memsub.promo.Promotion._
import model.{FreePlanChoice, OrientatedImages, PaidPlanChoice}
import org.pegdown.PegDownProcessor
import play.api.data.{Form, Forms}
import org.pegdown.Extensions._
import scalaz.syntax.std.option._
import scalaz.{Monad, \/}

object Promotions extends Controller {

  import TouchpointBackend.Normal.{catalog, promoService}

  private def getCheapestPaidMembershipPlan(promotion: PromoWithMembershipLandingPage) = {
    promotion.appliesTo.productRatePlanIds.toList.flatMap(rp => catalog.findPaid(rp))
      .sortBy(paidTier => paidTier.priceGBP.amount)
      .headOption
  }

  private def getTypeOfPaidTier(paidTier: PaidTier) = {
    catalog.findPaid(paidTier)
  }

  private def getImageForPromotionLandingPage(promotion: PromoWithMembershipLandingPage) =
    promotion.landingPage.image.getOrElse(ResponsiveImageGroup(availableImages = Seq.empty))

  private def getPageInfo(promotion: PromoWithMembershipLandingPage, url: String) = PageInfo(
    title = promotion.landingPage.title.getOrElse(promotion.name),
    url = url,
    description = promotion.landingPage.description orElse Some(promotion.description)
  )

  private def findTemplateForPromotion(promoCode: PromoCode, promotion: PromoWithMembershipLandingPage, url: String)() = {

    implicit val countryGroup = CountryGroup.UK

    val newsroom =
      ResponsiveImageGroup(None, None, None,
      ResponsiveImageGenerator("5821a16dc3d05611212d2692037ecc82384ef7e2/0_0_5376_2280", Seq(2000, 1000, 500))
    )

    val hero = OrientatedImages(
      portrait = promotion.landingPage.heroImage.map(_.image).getOrElse(newsroom),
      landscape = promotion.landingPage.heroImage.map(_.image).getOrElse(newsroom)
    )

    val pegdown = new PegDownProcessor(SUPPRESS_ALL_HTML)
    getCheapestPaidMembershipPlan(promotion).fold(Option.empty[Html]) {
      paidMembershipPlan => {
        promotion.promotionType match {
          case p: PercentDiscount =>
            val originalPrice = paidMembershipPlan.pricing.getPrice(countryGroup.currency).get
            val discountedPrice = p.applyDiscount(originalPrice, paidMembershipPlan.billingPeriod)
            Some(views.html.promotions.singlePricePlanDiscountLandingPage(hero,
              promotion.landingPage.heroImage.fold[HeroImageAlignment](Centre)(_.alignment),
              paidMembershipPlan,
              getTypeOfPaidTier(paidMembershipPlan.tier),
              getPageInfo(promotion, url),
              getImageForPromotionLandingPage(promotion),
              promoCode,
              promotion.copy(promotionType = p),
              originalPrice,
              discountedPrice,
              pegdown
            ))
          case _ =>
            Some(views.html.promotions.singleTierLandingPage(
              getTypeOfPaidTier(paidMembershipPlan.tier),
              getPageInfo(promotion, url),
              hero,
              promotion.landingPage.heroImage.fold[HeroImageAlignment](Centre)(_.alignment),
              getImageForPromotionLandingPage(promotion),
              promoCode,
              promotion,
              pegdown
            ))
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
        withLanding <- promotion.asMembership \/> redirectToHomepageWithSession
        html <- findTemplateForPromotion(promoCode, withLanding, request.path) \/> redirectToHomepageWithSession
        response <- \/.right(Ok(html).withCookies(sessionCookieFromCode(promoCode)))
      } yield response
     ).fold(identity, identity) //Whether or not the for comprehension succeeds, we have a result- which we want to return
  }


  def preview() = GoogleAuthenticatedStaffAction { implicit request =>
    Form(Forms.single("promoJson" -> Forms.text)).bindFromRequest().fold(_ => NotFound, { jsString =>
      Json.fromJson[AnyPromotion](Json.parse(jsString)).asOpt.flatMap(_.asMembership)
        .flatMap(p => findTemplateForPromotion(p.codes.headOption.getOrElse(PromoCode("NO-CODE")), p, request.path))
        .fold[Result](NotFound)(p => Ok(p))
    })
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
