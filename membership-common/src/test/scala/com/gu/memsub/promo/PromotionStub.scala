package com.gu.memsub.promo
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGroup}
import com.gu.memsub.promo.Promotion.AnyPromotion
import io.lemonlabs.uri.dsl._
import org.joda.time.DateTime

/**
  * Promotions are quite laborious to construct
  * So these are helper methods for unit tests
  */
object PromotionStub {

  implicit class PromoModifier(p: AnyPromotion) {
    def ofType[P <: PromotionType[PromoContext]](newType: P): Promotion[P, Option, LandingPage] = p.copy[P, Option, LandingPage](promotionType = newType)
    def withCampaign(newCampaign: String) = p.copy[PromotionType[PromoContext], Option, LandingPage](campaign = CampaignCode(newCampaign))
  }

  def promoFor(code: String, ids: ProductRatePlanId*): AnyPromotion = Promotion(
    name = "Test promotion",
    description = s"$code description",
    appliesTo = AppliesTo.ukOnly(ids.toSet),
    campaign = CampaignCode("C"),
    channelCodes = Map(Channel("testChannel") -> Set(PromoCode(code))),
    landingPage = Some(DigitalPackLandingPage(Some("Page"), Some("Desc"), Some("Roundel"), Some(ResponsiveImageGroup(availableImages = Seq(ResponsiveImage("http://example.com", 0)))), Some(Blue))),
    starts = DateTime.now().minusDays(1),
    expires = Some(DateTime.now().plusDays(1)),
    promotionType = Tracking
  )
}

