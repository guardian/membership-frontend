package controllers.rest

import model.Eventbrite.EBVenue
import model.RichEvent.RichEvent
import org.joda.time.DateTime
import play.api.libs.json.Json

case class Event(
  id: String,
  url: String,
  mainImageUrl: Option[String],
  socialImageUrl: Option[String],
  start: DateTime,
  end: DateTime,
  venue: Venue,
  title: String
)

case class Venue(name: Option[String], address: Option[Address])
case class Address(city: Option[String], postCode: Option[String], country: Option[String])

object Venue {
  implicit val addressWrites = Json.writes[Address]
  implicit val venueWrites = Json.writes[Venue]

  def forEventVenue(ebVenue: EBVenue): Venue = {
    val address = ebVenue.address map { a => Address(a.city, a.postal_code, a.country) }
    Venue(ebVenue.name, address)
  }
}


object Event {
  implicit val writesEvent = Json.writes[Event]

  def forRichEvent(e: RichEvent) = Event(
    id = e.underlying.ebEvent.id,
    url = e.underlying.ebEvent.memUrl,
    mainImageUrl = e.gridImgUrl,
    socialImageUrl = e.socialImgUrl,
    start = e.underlying.ebEvent.start,
    end = e.underlying.ebEvent.end,
    venue = Venue.forEventVenue(e.underlying.ebEvent.venue),
    title = e.underlying.ebEvent.name.text
  )
}

