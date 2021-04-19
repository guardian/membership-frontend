package model

import model.RichEvent.RichEvent
import play.api.libs.json.{Json, Writes}

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
  description: String,
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
    event.underlying.ebEvent.venue.name.map { name =>
      LocationSchema(name, event.underlying.ebEvent.venue.addressLine, event.underlying.ebEvent.venue.googleMapsLink)
    }
  }

  private def offerOpt(event: RichEvent): Option[OfferSchema] = {
    event.underlying.generalReleaseTicket.map { ticket =>
      OfferSchema(event.underlying.ebEvent.memUrl, "primary", ticket.priceValue, ticket.currencyCode, event.underlying.statusSchema)
    }
  }

  def from(event: RichEvent): EventSchema = EventSchema(
    event.underlying.ebEvent.name.text,
    event.underlying.ebDescription.cleanHtml,
    event.underlying.ebEvent.start.toString,
    event.underlying.ebEvent.end.toString,
    event.underlying.ebEvent.memUrl,
    event.socialImgUrl,
    locationOpt(event),
    offerOpt(event)
  )
}
