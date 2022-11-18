package model

import model.RichEvent.RichEvent
import play.api.libs.json.Json
import configuration.{Config, CopyConfig}

object LocationSchema {
  implicit val writesSchema = Json.writes[LocationSchema]
}

case class LocationSchema(
 name: Option[String],
 url:Option[String],
 address: Option[String],
 hasMap: Option[String],
 `@type`: String
                         )

object OfferSchema {
  implicit val writesSchema = Json.writes[OfferSchema]
}

case class OfferSchema(
 url: String,
 category: String,
 price: String,
 priceCurrency: String,
 validFrom:String,
 availability: Option[String],
 `@type`: String = "Offer")
object OrganizerSchema {
  implicit val writesSchema = Json.writes[OrganizerSchema]
}

case class OrganizerSchema(
                        url: String,
                        name: String,
                        `@type`: String = "Organization")

case class EventSchema(
 name: String,
 description: String,
 startDate: String,
 endDate: String,
 url: String,
 image: Option[String],
 eventAttendanceMode:String,
 eventStatus:String,
 location: Option[LocationSchema],
 offers: Option[OfferSchema],
 organizer:Option[OrganizerSchema],
 `@context`: String = "http://schema.org",
 `@type`: String = "Event")

object EventSchema {

  implicit val writesSchema = Json.writes[EventSchema]

  private def eventAttendanceMode(event:RichEvent)={
    if (event.underlying.ebEvent.venue.name.isEmpty)
      "https://schema.org/OnlineEventAttendanceMode"
    else
      "https://schema.org/MixedEventAttendanceMode"
  }

  private def locationOpt(event: RichEvent): Option[LocationSchema] = {
    event.underlying.ebEvent.venue.name match {
      case None => if (!event.underlying.isSoldOut)
          Some(LocationSchema(None, Some(event.underlying.ebEvent.memUrl), None, None, "VirtualLocation"))
        else
          Some(LocationSchema(None, Some(Config.eventbriteWaitlistUrl(event.underlying.ebEvent)), None, None, "VirtualLocation"))
      case Some(name) =>
        Some(LocationSchema(Some(name), None, event.underlying.ebEvent.venue.addressLine, event.underlying.ebEvent.venue.googleMapsLink, "Place"))
    }
  }

  private def offerOpt(event: RichEvent): Option[OfferSchema] = {
    event.underlying.generalReleaseTicket.map { ticket =>
      OfferSchema(event.underlying.ebEvent.memUrl, "primary", ticket.priceValue, ticket.currencyCode,ticket.sales_start.getOrElse(None).toString, event.underlying.statusSchema)
    }
  }

  private def organizerOpt(event: RichEvent): Option[OrganizerSchema] = {
      Some(OrganizerSchema(event.underlying.ebEvent.memUrl, "Guardian Members"))
  }


  def from(event: RichEvent): EventSchema = EventSchema(
    event.underlying.ebEvent.name.text,
    event.underlying.ebDescription.cleanHtml,
    event.underlying.ebEvent.start.toString,
    event.underlying.ebEvent.end.toString,
    event.underlying.ebEvent.memUrl,
    event.socialImgUrl,
    eventAttendanceMode(event),
    event.underlying.ebEvent.status,
    locationOpt(event),
    offerOpt(event),
    organizerOpt(event),
  )
}

