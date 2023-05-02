package com.gu.memsub.promo

import java.util
import java.util.UUID

import com.amazonaws.services.dynamodbv2.document.Item
import com.gu.i18n.Country
import com.gu.i18n.CountryGroup.{unapply => _, _}
import com.gu.memsub.Subscription.{ProductRatePlanId => PrpId}
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGroup}
import com.gu.memsub.promo.Promotion.AnyPromotion
import io.lemonlabs.uri.Uri
import org.joda.time.{DateTime, Days}
import play.api.libs.functional.syntax._

import scala.collection.JavaConverters._
import play.api.libs.json._

import scala.util.Try
import com.gu.memsub.images.GridDeserializer._
import com.gu.memsub.promo.CampaignGroup.{DigitalPack, GuardianWeekly, Membership, Newspaper}

import scala.collection.immutable.Map

object Formatters {

  implicit val ErrorWrites: Writes[PromoError] = new Writes[PromoError] {
    override def writes(e: PromoError) = Json.obj("errorMessage"-> e.msg)
  }

  object Common {

    implicit val prpIdFormat = Format.GenericFormat[String].inmap[PrpId](PrpId, _.get)
    implicit val countryFormat = Format.GenericFormat[String].inmap[Country](c => countryByCode(c).get, _.alpha2)
    implicit val DayFormat: Format[Days] = Format.GenericFormat[Int].inmap[Days](Days.days, _.getDays)
    implicit val uriFormat: Format[Uri] = Format.GenericFormat[String].inmap[Uri](Uri.parse, _.toString)

    val scalarCampaignCodeFormat = Format.GenericFormat[String].inmap[CampaignCode](CampaignCode, _.get)
    val uuidScalarFormat: Format[UUID] = Format.GenericFormat[String].inmap[UUID](UUID.fromString, _.toString)

    implicit val uuidOFormat: OFormat[UUID] = (__ \ "uuid").format[UUID](uuidScalarFormat)

    val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"
    implicit val dateTimeReads = JodaReads.jodaDateReads(dateFormat)
    implicit val dateTimeWrites = JodaWrites.jodaDateWrites(dateFormat)
  }

  object CampaignFormatters {

    import Common._

    implicit val campaignGroupFormat = OFormat(
      new Reads[CampaignGroup] {
        override def reads(json: JsValue): JsResult[CampaignGroup] = {
          // The only legacy serialisations of productFamily (in Campaign) will be migrated to use the CampaignGroup instead.
          val campaignGroupOpt = (json \ "productFamily").validate[String].asOpt orElse (json \ "group").validate[String].asOpt
          campaignGroupOpt.flatMap(CampaignGroup.fromId).fold[JsResult[CampaignGroup]](
            JsError(s"Bad campagin group ${campaignGroupOpt.mkString}")
          )(campaignGroupFormat => JsSuccess(campaignGroupFormat))
        }
      },
      new OWrites[CampaignGroup] {
        override def writes(o: CampaignGroup): JsObject = Json.obj("group" -> o.id)
      }
    )

    implicit val campaignCodeFormat: OFormat[CampaignCode] =
      (__ \ "code").format[CampaignCode](scalarCampaignCodeFormat)

    implicit val campaignFormat: OFormat[Campaign] = (
      __.format[CampaignCode] and
      __.format[CampaignGroup] and
      (__ \ "name").format[String] and
      (__ \ "sortDate").formatNullable[DateTime]
    )(Campaign, unlift(Campaign.unapply))
  }


  object PromotionFormatters {

    import Common._

    import com.gu.memsub.images.GridDeserializer._
    implicit val riFormat = Json.format[ResponsiveImage]
    implicit val rigFormat = Json.format[ResponsiveImageGroup]

    implicit val heroImageAlignmentFormat = Format(
      new Reads[HeroImageAlignment] {
        override def reads(json: JsValue): JsResult[HeroImageAlignment] = json match {
          case JsString("bottom") => JsSuccess(Bottom)
          case JsString("centre") => JsSuccess(Centre)
          case JsString("top") => JsSuccess(Top)
          case _ => JsError("Unknown HeroImageAlignment provided")
        }
      },
      new Writes[HeroImageAlignment] {
        override def writes(a: HeroImageAlignment): JsValue = a match {
          case Bottom => JsString("bottom")
          case Centre => JsString("centre")
          case Top => JsString("top")
        }
      }
    )

    implicit val heroImage = Json.format[HeroImage]
    implicit val sectionColourPageFormat = Format(
      new Reads[SectionColour] {
        override def reads(json: JsValue): JsResult[SectionColour] = json match {
          case JsString("blue") => JsSuccess(Blue)
          case JsString("grey") => JsSuccess(Grey)
          case JsString("white") => JsSuccess(White)
          case _ => JsError("Unknown SectionColour provided")
        }
      },
      new Writes[SectionColour] {
        override def writes(s: SectionColour): JsValue = s match {
          case Blue => JsString("blue")
          case Grey => JsString("grey")
          case White => JsString("white")
        }
      }
    )

    implicit val countrySetFormat = Format(Reads.set[Country], Writes.set[Country])
    implicit val prpIdSetFormat = Format(Reads.set[PrpId], Writes.set[PrpId])

    implicit val landingPageFormat = Format(
      new Reads[LandingPage] {
        // Supports the legacy serialisations of productFamily key as the identifier of a LandingPage type
        override def reads(json: JsValue): JsResult[LandingPage] = (json \ "productFamily").toOption orElse (json \ "type").toOption match {
          case Some(JsString(Membership.id)) => Json.reads[MembershipLandingPage].reads(json)
          case Some(JsString(DigitalPack.id))  => Json.reads[DigitalPackLandingPage].reads(json)
          case Some(JsString(Newspaper.id))  => Json.reads[NewspaperLandingPage].reads(json)
          case Some(JsString(GuardianWeekly.id)) => Json.reads[WeeklyLandingPage].reads(json)
          case _ => JsError("Unknown landing page type")
        }
      },
      new OWrites[LandingPage] {
        def writes(in: LandingPage): JsObject = {
          in match {
            case mlp: MembershipLandingPage => Json.writes[MembershipLandingPage].writes(mlp) ++ Json.obj("type" -> Membership.id)
            case dlp: DigitalPackLandingPage => Json.writes[DigitalPackLandingPage].writes(dlp) ++ Json.obj("type" -> DigitalPack.id)
            case nlp: NewspaperLandingPage => Json.writes[NewspaperLandingPage].writes(nlp) ++ Json.obj("type" -> Newspaper.id)
            case wlp: WeeklyLandingPage => Json.writes[WeeklyLandingPage].writes(wlp) ++ Json.obj("type" -> GuardianWeekly.id)
          }
        }
      }
    )

    implicit val membershipLandingPageFormat: Format[MembershipLandingPage] = Json.format[MembershipLandingPage]
    implicit val digitalpackLandingPageFormat = Json.format[DigitalPackLandingPage]
    implicit val newspaperLandingPageFormat = Json.format[NewspaperLandingPage]

    implicit val appliesToFormat = Json.format[AppliesTo]

    implicit val promoCodeFormat = Format.GenericFormat[String].inmap[PromoCode](PromoCode, _.get)
    implicit val promoCodeScalaSetFormat = Format(Reads.set[PromoCode], Writes.set[PromoCode])

    implicit val promoCodeMapReads = Reads.map[Set[PromoCode]].map { m =>
      m.map { case (key, code) => (Channel(key), code)}
    }

    implicit val promoCodeMapWrites = Writes.map[Set[PromoCode]].contramap[Map[Channel, Set[PromoCode]]] { m =>
      m.map { case (key, code) => (key.get, code)}
    }

    implicit val campaignCodeFormat: OFormat[CampaignCode] =
      (__ \ "campaignCode").format[CampaignCode](scalarCampaignCodeFormat)

    val trackingReads = new Reads[Tracking.type] {
      def reads(in: JsValue): JsResult[Tracking.type] = {
        (in \ "name").toOption match {
          case Some(JsString(Tracking.name)) => JsSuccess(Tracking)
          case _ => JsError(s"expected Tracking, got $in")
        }
      }
    }
    val retentionReads = new Reads[Retention.type] {
      def reads(in: JsValue): JsResult[Retention.type] = {
        (in \ "name").toOption match {
          case Some(JsString(Retention.name)) => JsSuccess(Retention)
          case _ => JsError(s"expected Retention, got $in")
        }
      }
    }


    implicit val doubleReads = new Reads[DoubleType[PromoContext]] {
      override def reads(json: JsValue): JsResult[DoubleType[PromoContext]] = (json \ "a", json \ "b") match {
        case (JsDefined(a), JsDefined(b)) => (promotionTypeFormat.reads(a) and promotionTypeFormat.reads(b))(DoubleType.apply[PromoContext] _)
        case a => JsError(s"Unable to read double promotion from $a")
      }
    }

    implicit val promotionTypeFormat: OFormat[PromotionType[PromoContext]] = OFormat(
      new Reads[PromotionType[PromoContext]] {
        override def reads(json: JsValue): JsResult[PromotionType[PromoContext]] = (json \ "name").validate[String] match {
          case JsSuccess(PromotionType.percentDiscount, _) => Json.reads[PercentDiscount].reads(json)
          case JsSuccess(PromotionType.double, _) => doubleReads.reads(json)
          case JsSuccess(PromotionType.incentive, _) => Json.reads[Incentive].reads(json)
          case JsSuccess(PromotionType.freeTrial, _) => Json.reads[FreeTrial].reads(json)
          case JsSuccess(PromotionType.tracking, _) => JsSuccess(Tracking)
          case JsSuccess(PromotionType.retention, _) => JsSuccess(Retention)
          case _ => JsError(s"Failed to deserialise $json as a promo type")
        }
      },
      new OWrites[PromotionType[PromoContext]] {
        def writes(in: PromotionType[PromoContext]): JsObject = {
          val o: JsObject = in match {
            case p: PercentDiscount => Json.writes[PercentDiscount].writes(p)
            case DoubleType(a, b) => Json.obj(
              "a" -> promotionTypeFormat.writes(a),
              "b" -> promotionTypeFormat.writes(b),
              "name" -> PromotionType.double
            )
            case f: FreeTrial => Json.writes[FreeTrial].writes(f)
            case i: Incentive => Json.writes[Incentive].writes(i)
            case r: Retention.type => Json.obj()
            case t: Tracking.type => Json.obj()
          }
          o ++ Json.obj("name" -> in.name)
        }
      }
    )

    implicit val promotionFormat: OFormat[AnyPromotion] = (
        __.format[UUID] and
        (__ \ "name").format[String] and
        (__ \ "description").format[String] and
        (__ \ "appliesTo").format[AppliesTo] and
        __.format[CampaignCode] and
        (__ \ "codes").format[Map[Channel, Set[PromoCode]]] and
        (__ \ "landingPage").formatNullable[LandingPage] and
        (__ \ "starts").format[DateTime] and
        (__ \ "expires").formatNullable[DateTime] and
        (__ \ "promotionType").format[PromotionType[PromoContext]]
      ) (Promotion.apply[PromotionType[PromoContext], Option, LandingPage], unlift(Promotion.unapply[PromotionType[PromoContext], Option, LandingPage]))
  }
}
