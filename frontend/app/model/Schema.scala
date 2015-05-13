package model

import model.RichEvent.RichEvent
import play.api.libs.json.Json

object LocationSchema {
  implicit val writesSchema = Json.writes[LocationSchema]
}

case class LocationSchema(
  name: String,
  address: Option[String],
  hasMap: Option[String],
  `@type`: String = "Place"
)

object OfferSchema {
  implicit val writesSchema = Json.writes[OfferSchema]
}

case class OfferSchema(
  url: String,
  category: String,
  price: String,
  priceCurrency: String,
  availability: Option[String],
  `@type`: String = "Offer"
)

case class EventSchema(
  name: String,
  description: Option[String],
  startDate: String,
  endDate: String,
  url: String,
  image: Option[String],
  location: Option[LocationSchema],
  offers: Option[OfferSchema],
  `@context`: String = "http://schema.org",
  `@type`: String = "Event"
)

object EventSchema {

  implicit val writesSchema = Json.writes[EventSchema]

  private def locationOpt(event: RichEvent): Option[LocationSchema] = {
    event.venue.name.map { name =>
      LocationSchema(name, event.venue.addressLine, event.venue.googleMapsLink)
    }
  }

  private def offerOpt(event: RichEvent): Option[OfferSchema] = {
    event.generalReleaseTicket.map { ticket =>
      OfferSchema(event.memUrl, "primary", ticket.priceValue, ticket.currencyCode, event.statusSchema)
    }
  }

  def from(event: RichEvent): EventSchema = EventSchema(
    event.name.text,
    event.description.map(_.text),
    event.start.toString,
    event.end.toString,
    event.memUrl,
    event.socialImgUrl,
    locationOpt(event),
    offerOpt(event)
  )
}
