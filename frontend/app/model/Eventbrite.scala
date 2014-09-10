package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

import com.github.nscala_time.time.Imports._

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.Instant

import configuration.Config
import utils.StringUtils.truncateToWordBoundary

object Eventbrite {

  trait EBObject

  object EBEventStatus extends Enumeration {
    type EBEventStatus = Value
    val Completed, Cancelled, SoldOut, PreLive, Live = Value
  }

  case class EBError(error: String, error_description: String, status_code: Int) extends Throwable with EBObject {
    override def getMessage: String = s"$status_code $error - $error_description"
  }

  case class EBResponse[T](pagination: EBPagination, data: Seq[T]) extends EBObject

  case class EBPagination(object_count: Int,
                          page_number: Int,
                          page_size: Int,
                          page_count: Int) extends EBObject {
    lazy val nextPageOpt = Some(page_number + 1).filter(_ <= page_count)
  }

  case class EBRichText(text: String, html: String) {
    def cleanHtml: String = {
      val stylePattern = "(?i)style=(\".*?\"|'.*?'|[^\"'][^\\s]*)".r
      val cleanStyle = stylePattern replaceAllIn(html, "")
      val clean = "(?i)<br>".r.replaceAllIn(cleanStyle, "")
      clean
    }

    lazy val blurb = truncateToWordBoundary(text, 200)
  }

  case class EBAddress(country_name: Option[String],
                       city: Option[String],
                       address_1: Option[String],
                       address_2: Option[String],
                       region: Option[String],
                       country: Option[String],
                       postal_code: Option[String]) extends EBObject

  case class EBVenue(id: Option[String],
                     address: Option[EBAddress],
                     latitude: Option[String],
                     longitude: Option[String],
                     name: Option[String]) extends EBObject

  case class EBPricing(currency: String, display: String, value: Int) extends EBObject {
    def priceFormat(priceInPence: Double) = "£" + f"${priceInPence/100}%2.0f".trim

    lazy val formattedPrice = priceFormat(value)

    lazy val discountPrice = priceFormat(value * Config.discountMultiplier)

    lazy val savingPrice = priceFormat(value * (1-Config.discountMultiplier))
  }


  /**
   * https://developer.eventbrite.com/docs/ticket-class-object/
   */
  case class EBTickets(id: Option[String] = None,
                       name: Option[String] = None,
                       free: Boolean = false,
                       quantity_total: Option[Int] = None,
                       quantity_sold: Option[Int] = None,
                       cost: Option[EBPricing] = None,
                       sales_end: Option[Instant] = None,
                       sales_start: Option[Instant] = None,
                       hidden: Option[Boolean] = None) extends EBObject

  case class EBEvent(
                      name: EBRichText,
                      description: Option[EBRichText],
                      logo_url: Option[String],
                      url: String,
                      id: String,
                      start: DateTime,
                      end: DateTime,
                      venue: EBVenue,
                      capacity: Option[Int],
                      ticket_classes: Seq[EBTickets],
                      status: Option[String]) extends EBObject {

    lazy val logoUrl = logo_url.map(_.replace("http:", ""))

    lazy val countryName = venue.address.flatMap(_.country_name).getOrElse("")

    lazy val city = venue.address.flatMap(_.city).getOrElse("")

    lazy val addressOne = venue.address.flatMap(_.address_1).getOrElse("")

    lazy val addressTwo = venue.address.flatMap(_.address_2).getOrElse("")

    lazy val region = venue.address.flatMap(_.region).getOrElse("")

    lazy val country = venue.address.flatMap(_.country).getOrElse("")

    lazy val postal_code = venue.address.flatMap(_.postal_code).getOrElse("")

    import EBEventStatus._

    def getStatus: EBEventStatus = {
      val numberSoldTickets = ticket_classes.flatMap(_.quantity_sold).sum

      status.collect {
        case "completed" => Completed

        case "canceled" => Cancelled // American spelling

        case "live" if numberSoldTickets >= capacity.getOrElse(0) => SoldOut

        case "live" => {
          val startDates = ticket_classes.map(_.sales_start.getOrElse(Instant.now))
          if (startDates.exists(_ <= Instant.now)) Live else PreLive
        }

        case "draft" => PreLive

        case _ => Live
      }.getOrElse(PreLive)
    }

    // This currently extracts all none hidden tickets and gets the first one
    def ticketClassesHead = ticket_classes.find(_.hidden.getOrElse(false) == false)
  }

  case class EBDiscount(code: String, quantity_available: Int, quantity_sold: Int) extends EBObject
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

  implicit val instant: Reads[Instant] = JsPath.read[String].map(convertInstantText)

  implicit val readsEbDate: Reads[DateTime] = (
    (JsPath \ "utc").read[String] and
      (JsPath \ "timezone").read[String]
    )(convertDateText _)

  implicit val ebError = Json.reads[EBError]
  implicit val ebAddress = Json.reads[EBAddress]
  implicit val ebVenue = Json.reads[EBVenue]

  implicit val ebRichText: Reads[EBRichText] = (
      (JsPath \ "text").readNullable[String].map(_.getOrElse("")) and
        (JsPath \ "html").readNullable[String].map(_.getOrElse(""))
    )(EBRichText.apply _)

  implicit val ebPricingReads = Json.reads[EBPricing]
  implicit val ebTicketsReads = Json.reads[EBTickets]
  implicit val ebEventReads = Json.reads[EBEvent]
  implicit val ebDisountReads = Json.reads[EBDiscount]

  implicit val ebPaginationReads = Json.reads[EBPagination]
  implicit val ebEventsReads = ebResponseReads[EBEvent]("events")
  implicit val ebDiscountsReads = ebResponseReads[EBDiscount]("discounts")
}
