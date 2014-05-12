
package model

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{ Reads, Json }
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.Instant

case class EBRichText(text: String, html: String)
case class EBAddress(country_name: Option[String], city: Option[String], address_1: Option[String], address_2: Option[String], region: Option[String], country: Option[String])
case class EBVenue(id: Option[String], address: EBAddress, latitude: Option[String], longitude: Option[String], name: Option[String])
case class EBResponse(events: Seq[EBEvent])
case class EBPricing(currency: String, display: String, value: Int)
case class EBTickets(id: Option[String], name: Option[String], free: Option[Boolean], quantity_total: Option[Int], quantity_sold: Option[Int], cost: Option[EBPricing], sales_end: Option[Instant])
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
    ticket_classes: Option[Seq[EBTickets]]) {
  val blankAddress = EBAddress(None, None, None, None, None, None)

  def countryName = venue.address.country_name.getOrElse("")
  def city = venue.address.city.getOrElse("")
  def addressOne = venue.address.address_1.getOrElse("")
  def addressTwo = venue.address.address_2.getOrElse("")
  def region = venue.address.region.getOrElse("")
  def country = venue.address.country.getOrElse("")
}

object EventbriteDeserializer {

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

  implicit val ebAddress = Json.reads[EBAddress]
  implicit val ebVenue = Json.reads[EBVenue]
  implicit val ebRichText = Json.reads[EBRichText]
  implicit val ebPricingReads = Json.reads[EBPricing]
  implicit val ebTicketsReads = Json.reads[EBTickets]
  implicit val ebEventReads = Json.reads[EBEvent]
  implicit val ebResponseReads = Json.reads[EBResponse]
}
