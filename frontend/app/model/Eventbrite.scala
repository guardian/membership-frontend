package model

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{ Reads, Json }
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.Instant

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
  }

  case class EBAddress(country_name: Option[String],
                       city: Option[String],
                       address_1: Option[String],
                       address_2: Option[String],
                       region: Option[String],
                       country: Option[String]) extends EBObject

  case class EBVenue(id: Option[String],
                     address: Option[EBAddress],
                     latitude: Option[String],
                     longitude: Option[String],
                     name: Option[String]) extends EBObject

  case class EBPricing(currency: String, display: String, value: Int) extends EBObject

  case class EBTickets(id: Option[String],
                       name: Option[String],
                       free: Option[Boolean],
                       quantity_total: Option[Int],
                       quantity_sold: Option[Int],
                       cost: Option[EBPricing],
                       sales_end: Option[Instant],
                       sales_start: Option[Instant]) extends EBObject

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
                      ticket_classes: Option[Seq[EBTickets]],
                      status: Option[String]) extends EBObject {
    val blankAddress = EBAddress(None, None, None, None, None, None)

    def countryName = venue.address.getOrElse(blankAddress).country_name.getOrElse("")

    def city = venue.address.getOrElse(blankAddress).city.getOrElse("")

    def addressOne = venue.address.getOrElse(blankAddress).address_1.getOrElse("")

    def addressTwo = venue.address.getOrElse(blankAddress).address_2.getOrElse("")

    def region = venue.address.getOrElse(blankAddress).region.getOrElse("")

    def country = venue.address.getOrElse(blankAddress).country.getOrElse("")

    import EBEventStatus._

    def getStatus: EBEventStatus = {
      val numberSoldTickets = ticket_classes.getOrElse(Seq.empty).flatMap(_.quantity_sold).sum

      status match {
        case Some("completed") => Completed

        case Some("canceled") => Cancelled // American spelling

        case Some("live") if numberSoldTickets >= capacity.getOrElse(0) => SoldOut

        case Some("live") => {
          val startDates = ticket_classes.getOrElse(Seq.empty).map(_.sales_start.getOrElse(Instant.now))
          if (startDates.exists(_ <= Instant.now)) {
            Live
          } else {
            PreLive
          }
        }

        case _ => Live
      }
    }

    def ticketClassesHead = ticket_classes.getOrElse(Seq.empty).headOption
  }

  case class EBDiscount(code: String) extends EBObject
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
  implicit val ebRichText = Json.reads[EBRichText]
  implicit val ebPricingReads = Json.reads[EBPricing]
  implicit val ebTicketsReads = Json.reads[EBTickets]
  implicit val ebEventReads = Json.reads[EBEvent]
  implicit val ebDisountReads = Json.reads[EBDiscount]

  implicit val ebPaginationReads = Json.reads[EBPagination]
  implicit val ebEventsReads = ebResponseReads[EBEvent]("events")
  implicit val ebDiscountsReads = ebResponseReads[EBDiscount]("discounts")
}
