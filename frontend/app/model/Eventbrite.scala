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

  sealed trait EBEventStatus

  sealed trait DisplayableEvent extends EBEventStatus

  case object Completed extends EBEventStatus
  case object Cancelled extends EBEventStatus
  case object SoldOut extends DisplayableEvent
  case object Live extends DisplayableEvent
  case object PreLive extends DisplayableEvent
  case object Draft extends EBEventStatus


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

  case class EBLocation(address_1: Option[String],
                       address_2: Option[String],
                       city: Option[String],
                       region: Option[String],
                       postal_code: Option[String],
                       country: Option[String],
                       latitude: Option[String],
                       longitude: Option[String]) extends EBObject

  case class EBVenue(id: Option[String],
                     location: Option[EBLocation],
                     name: Option[String]) extends EBObject

  case class EBPricing(currency: String, display: String, value: Int) extends EBObject {
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
                      created: Instant,
                      venue: EBVenue,
                      capacity: Option[Int],
                      ticket_classes: Seq[EBTickets],
                      status: String) extends EBObject {

    lazy val logoUrl = logo_url.map(_.replace("http:", ""))

    lazy val city = venue.location.flatMap(_.city).getOrElse("")

    lazy val addressOne = venue.location.flatMap(_.address_1).getOrElse("")

    lazy val addressTwo = venue.location.flatMap(_.address_2).getOrElse("")

    lazy val region = venue.location.flatMap(_.region).getOrElse("")

    lazy val country = venue.location.flatMap(_.country).getOrElse("")

    lazy val postal_code = venue.location.flatMap(_.postal_code).getOrElse("")

    lazy val eventAddressLine = Seq(
      addressOne,
      addressTwo,
      city,
      region,
      postal_code
    ).filter(_.nonEmpty).mkString(", ")

    lazy val isSoldOut = getStatus == SoldOut

    def getStatus: EBEventStatus = {
      val isSoldOut = ticket_classes.flatMap(_.quantity_sold).sum >= capacity.getOrElse(0)
      val isTicketSalesStarted = ticket_classes.exists(_.sales_start.forall(_ <= Instant.now))


      status match {
        case "completed" | "started" | "ended" => Completed
        case "canceled" => Cancelled // American spelling
        case "live" if isSoldOut => SoldOut
        case "live" if isTicketSalesStarted => Live
        case "draft" => Draft
        case _ => PreLive
      }
    }

    lazy val isNoTicketEvent = description.exists(_.html.contains("<!-- noTicketEvent -->"))

    lazy val visibleTicketClasses = ticket_classes.filterNot(_.hidden.getOrElse(false))

    lazy val ticketSalesEndOpt = visibleTicketClasses.flatMap(_.sales_end).sorted.headOption

    // This currently gets the first non-hidden ticket class
    def ticketClassesHead = visibleTicketClasses.headOption
  }

  case class EBDiscount(code: String, quantity_available: Int, quantity_sold: Int) extends EBObject

  //https://developer.eventbrite.com/docs/order-object/
  case class EBOrder(id: String, first_name: String, email: String, costs: EBCosts, attendees: Seq[EBAttendee]) extends EBObject {
    val ticketCount = attendees.length
    val totalCost = costs.gross.value
  }

  case class EBCosts(gross: EBCost) extends EBObject

  case class EBCost(value: Int) extends EBObject

  case class EBAttendee(quantity: Int) extends EBObject
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
  implicit val ebLocation = Json.reads[EBLocation]
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

  implicit val ebCostReads = Json.reads[EBCost]
  implicit val ebCostsReads = Json.reads[EBCosts]
  implicit val ebAttendeeReads = Json.reads[EBAttendee]
  implicit val ebOrderReads = Json.reads[EBOrder]
}
