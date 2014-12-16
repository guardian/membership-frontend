package model

import com.netaporter.uri.Uri
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError

import com.github.nscala_time.time.Imports._

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.Instant

import configuration.Config
import services.MasterclassData
import utils.StringUtils.truncateToWordBoundary
import com.netaporter.uri.dsl._

object Eventbrite {

  val googleMapsUri = Uri.parse("https://maps.google.com/")

  trait EBObject

  case class EBError(error: String, error_description: String, status_code: Int) extends Throwable with EBObject {
    override def getMessage: String = s"$status_code $error - $error_description"
  }

  case class EBResponse[T](pagination: EBPagination, data: Seq[T]) extends EBObject

  case class EBPagination(page_number: Int, page_count: Int) extends EBObject {
    lazy val nextPageOpt = Some(page_number + 1).filter(_ <= page_count)
  }

  case class EBRichText(text: String, html: String) {
    def cleanHtml: String = {
      val stylePattern = "(?i)style=(\".*?\"|'.*?'|[^\"'][^\\s]*)".r
      val cleanStyle = stylePattern replaceAllIn(html, "")
      val clean = "(?i)<br>".r.replaceAllIn(cleanStyle, "")

      // Remove Masterclass return URL
      val mcPattern = "(?i)<a[^>]+>Full course and returns information on the Masterclasses website</a>".r
      mcPattern.replaceAllIn(clean, "")
    }

    lazy val blurb = truncateToWordBoundary(text, 120)
  }

  case class EBAddress(address_1: Option[String],
                       address_2: Option[String],
                       city: Option[String],
                       region: Option[String],
                       postal_code: Option[String],
                       country: Option[String]) extends EBObject {

    lazy val toSeq: Seq[String] = Seq(address_1, address_2, city, region, postal_code).flatten

    lazy val asLine: Option[String] = if (toSeq.isEmpty) None else Some(toSeq.mkString(", "))
  }

  case class EBVenue(address: Option[EBAddress], name: Option[String]) extends EBObject {
    lazy val addressLine = address.flatMap(_.asLine)

    lazy val googleMapsLink: Option[String] =
      addressLine.map(al => googleMapsUri ? ("q" -> (name.map(_ + ", ").mkString + al)))
  }

  case class EBPricing(value: Int) extends EBObject {
    def priceFormat(priceInPence: Double) = {
      val priceInPounds = priceInPence.round / 100f
      if (priceInPounds.isWhole) f"£$priceInPounds%.0f" else f"£$priceInPounds%.2f"
    }

    lazy val formattedPrice = priceFormat(value)
    lazy val discountPrice = priceFormat(value * Config.discountMultiplier)
    lazy val savingPrice = priceFormat(value * (1 - Config.discountMultiplier))
  }


  /**
   * https://developer.eventbrite.com/docs/ticket-class-object/
   */
  case class EBTicketClass(id: String,
                       name: String,
                       free: Boolean,
                       quantity_total: Int,
                       quantity_sold: Int,
                       cost: Option[EBPricing],
                       sales_end: Instant,
                       sales_start: Option[Instant],
                       hidden: Option[Boolean]) extends EBObject {
    val isHidden = hidden.exists(_ == true)
  }

  case class EBEvent(name: EBRichText,
                     description: Option[EBRichText],
                     url: String,
                     id: String,
                     start: DateTime,
                     end: DateTime,
                     created: Instant,
                     venue: EBVenue,
                     capacity: Int,
                     ticket_classes: Seq[EBTicketClass],
                     status: String) extends EBObject {

    val isSoldOut = ticket_classes.map(_.quantity_sold).sum >= capacity
    val isNoTicketEvent = description.exists(_.html.contains("<!-- noTicketEvent -->"))
    val isBookable = status == "live" && !isSoldOut

    val generalReleaseTicket = ticket_classes.find(!_.isHidden)
    val memberTickets = ticket_classes.filter { t => t.isHidden && t.name.toLowerCase.startsWith("guardian member") }

    lazy val memUrl = Config.membershipUrl + controllers.routes.Event.details(id)
  }

  trait EBCode extends EBObject {
    val code: String
    val quantity_available: Int
  }

  case class EBDiscount(code: String, quantity_available: Int, quantity_sold: Int) extends EBCode
  case class EBAccessCode(code: String, quantity_available: Int) extends EBCode

  //https://developer.eventbrite.com/docs/order-object/
  case class EBOrder(id: String, first_name: String, email: String, costs: EBCosts, attendees: Seq[EBAttendee]) extends EBObject {
    val ticketCount = attendees.length
    val totalCost = costs.gross.value / 100f
  }

  case class EBCosts(gross: EBCost) extends EBObject

  case class EBCost(value: Int) extends EBObject

  case class EBAttendee(quantity: Int) extends EBObject

  trait RichEvent {
    val event: EBEvent
    val imgUrl: String
    val socialImgUrl: String
    val tags: Seq[String]

    val maxDiscounts: Int
    val allowDiscountCodes: Boolean
  }

  object RichEvent {
    implicit def eventToEBEvent(event: RichEvent) = event.event
    implicit def eventOptToEBEventOpt(eventOpt: Option[RichEvent]) = eventOpt.map(_.event)
  }

  case class GuLiveEvent(event: EBEvent) extends RichEvent {
    val imgUrl = Config.eventImageUrlPath(event.id) + "/{width}{pixel_ratio}.jpg"
    val socialImgUrl = {
      // get the largest available image (for social media etc)
      // this mirrors the URL structure generated by Imager.js
      // so any changes there need to be reflected here
      val largestWidth = Config.eventImageWidths.toArray.last
      val largestRatio = Config.eventImageRatios.toArray.last
      Config.eventImageUrlPath(event.id) + "/" + largestWidth + "-" + largestRatio + "x.jpg"
    }

    val tags = Nil

    val maxDiscounts = 2
    val allowDiscountCodes = true
  }

  case class MasterclassEvent(event: EBEvent, data: Option[MasterclassData]) extends RichEvent {
    val imgUrl = data.flatMap(_.images.headOption).flatMap(_.file)
      .getOrElse(views.support.Asset.at("images/event-placeholder.gif"))
      .replace("http://static", "https://static-secure")
    val socialImgUrl = imgUrl

    val tags = event.description.map(_.html).flatMap(MasterclassEvent.extractTags).getOrElse(Nil)

    val maxDiscounts = 1
    val allowDiscountCodes = false
  }


  object MasterclassEvent {
    case class tagItem(categoryName: String, subCategories: Seq[String] = Seq())

    val tags = Seq(
      tagItem("Writing", Seq("Copywriting", "Creative writing", "Research", "Fiction", "Non-fiction")),
      tagItem("Publishing"),
      tagItem("Journalism"),
      tagItem("Business"),
      tagItem("Digital"),
      tagItem("Culture"),
      tagItem("Food and drink"),
      tagItem("Media")
    )

    // if a tag is hyphenated (Non-fiction) then weirdness/duplication happens here
    // so we replace it with an underscore in URLs (ugly, but limited alternatives)
    def encodeTag(tag: String) = tag.toLowerCase.replace("-", "_").replace(" ", "-")
    def decodeTag(tag: String) = tag.capitalize.replace("-", " ").replace("_", "-")

    def extractTags(s: String): Option[Seq[String]] =
      "<!--\\s*tags:(.*?)-->".r.findFirstMatchIn(s).map(_.group(1).split(",").toSeq.map(_.trim.toLowerCase))
  }
}

object EventbriteDeserializer {
  import Eventbrite._

  private def ebResponseReads[T](namespace: String)(implicit reads: Reads[Seq[T]]): Reads[EBResponse[T]] =
    ((JsPath \ "pagination").read[EBPagination] and
      (JsPath \ namespace).read[Seq[T]])(EBResponse[T] _)

  private def convertInstantText(utc: String): Instant =
    ISODateTimeFormat.dateTimeNoMillis.parseDateTime(utc).toInstant

  private def convertDateText(utc: String, timezone: String): DateTime = {
    val timeZone = DateTimeZone.forID(timezone)
    ISODateTimeFormat.dateTimeNoMillis.parseDateTime(utc).withZone(timeZone)
  }

  // Remove any leading/trailing spaces left by the events team
  implicit val readsTrimString = Reads[String] {
    case JsString(s) => JsSuccess(s.trim)
    case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
  }

  implicit val instant: Reads[Instant] = JsPath.read[String].map(convertInstantText)

  implicit val readsEbDate: Reads[DateTime] = (
    (JsPath \ "utc").read[String] and
      (JsPath \ "timezone").read[String]
    )(convertDateText _)

  implicit val ebError = Json.reads[EBError]
  implicit val ebLocation = Json.reads[EBAddress]
  implicit val ebVenue = Json.reads[EBVenue]

  implicit val ebRichText: Reads[EBRichText] = (
      (JsPath \ "text").readNullable[String].map(_.getOrElse("")) and
        (JsPath \ "html").readNullable[String].map(_.getOrElse(""))
    )(EBRichText.apply _)

  implicit val ebPricingReads = Json.reads[EBPricing]
  implicit val ebTicketsReads = Json.reads[EBTicketClass]
  implicit val ebEventReads = Json.reads[EBEvent]
  implicit val ebDiscountReads = Json.reads[EBDiscount]
  implicit val ebAccessCodeReads = Json.reads[EBAccessCode]

  implicit val ebPaginationReads = Json.reads[EBPagination]
  implicit val ebEventsReads = ebResponseReads[EBEvent]("events")
  implicit val ebDiscountsReads = ebResponseReads[EBDiscount]("discounts")
  implicit val ebAccessCodesReads = ebResponseReads[EBAccessCode]("access_codes")

  implicit val ebCostReads = Json.reads[EBCost]
  implicit val ebCostsReads = Json.reads[EBCosts]
  implicit val ebAttendeeReads = Json.reads[EBAttendee]
  implicit val ebOrderReads = Json.reads[EBOrder]
}
