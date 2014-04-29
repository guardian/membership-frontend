package model

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, Json, JsValue}
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat

object EventbriteEvent {

  case class EBRichText(text: String, html: String)

  case class EBAddress(country_name: Option[String], city: Option[String], region: Option[String], address_1: Option[String], country: Option[String])

  case class EBVenue(id: Option[String], address: EBAddress, latitude: Option[String], longitude: Option[String], name: Option[String])

  // case class EBTime(timezone: String, local: String, utc: String)

  case class EBEvent(
                      name: EBRichText,
                      description: EBRichText,
                      logo_url: String,
                      id: String,
                      start: DateTime,
                      end: DateTime,
                      venue: EBVenue) // Single event (event page)

  case class EBResponse(events: Seq[EBEvent]) // Array of events (events index)

  implicit val ebAddress = Json.reads[EBAddress]

  implicit val ebVenue = Json.reads[EBVenue]

  //implicit val ebTime = Json.reads[DateTime]

  def convertDateText(utc: String, timezone: String): DateTime = {
    val timeZone = DateTimeZone.forID(timezone)
    ISODateTimeFormat.dateTimeNoMillis.parseDateTime(utc).withZone(timeZone)
  }

  implicit val readsEbDate: Reads[DateTime] = (
    (JsPath \ "utc").read[String] and
      (JsPath \ "timezone").read[String]
    )(convertDateText _)

  implicit val ebRichText = Json.reads[EBRichText]

  implicit val ebEventReads = Json.reads[EBEvent]

  implicit val ebResponseReads = Json.reads[EBResponse]

}