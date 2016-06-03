package controllers.rest

import EventApi.EventsResponse
import model.Eventbrite.{EBAddress, EBEvent, EBRichText, EBVenue}
import com.gu.memsub.images.Grid
import com.gu.memsub.images.Grid.{Dimensions, Asset}
import model.RichEvent.{GridImage, GuLiveEvent}
import org.joda.time.DateTimeUtils._
import org.joda.time.DateTimeZone.UTC
import org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis
import org.joda.time.{DateTime, Instant}
import org.specs2.execute.{Result, AsResult}
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.AroundEach
import play.api.libs.json.Json

class EventsResponseTest extends Specification with JsonMatchers with EventFixtures {
  "Event response JSON created from RichEvent" should {
    "have event id" in {
      val response = EventsResponse(events = Seq(Event.forRichEvent(defaultLiveEvent)))

      asJsonString(response) must /("events") /# 0 / ("id" -> defaultLiveEvent.id)
    }

    "have url" in {
      val response = EventsResponse(events = Seq(Event.forRichEvent(defaultLiveEvent)))

      asJsonString(response) must /("events") /# 0 / ("url" -> contain("/event/"))
    }

    "have title" in {
      val response = EventsResponse(events = Seq(Event.forRichEvent(defaultLiveEvent)))

      asJsonString(response) must /("events") /# 0 / ("title" -> defaultLiveEvent.name.text)
    }

    "have start and dates in utc with timezone" in {
      val response = EventsResponse(events = Seq(Event.forRichEvent(defaultLiveEvent)))

      val expectedStartTime = defaultLiveEvent.start
        .toDateTime(UTC)
        .toString(dateTimeNoMillis())

      asJsonString(response) must /("events") /# 0 / "start" / ("time" -> expectedStartTime)
    }

    "have venue" in {
      val venue = EBVenue(
        address = Some(EBAddress(
          city = Some("London"),
          postal_code = Some("N1 6GU"),
          country = Some("GB"),
          region = None, address_1 = None, address_2 = None)
        ),
        name = Some("Kings Cross")
      )

      val response = EventsResponse(events = Seq(Event.forRichEvent(liveEvent(venue))))

      asJsonString(response) must /("events") /# 0 / "venue" / "address" / ("city" -> "London")
      asJsonString(response) must /("events") /# 0 / "venue" / "address" / ("postCode" -> "N1 6GU")
      asJsonString(response) must /("events") /# 0 / "venue" / "address" / ("country" -> "GB")
    }

    "have masterImageUrl" in {
      val response = EventsResponse(events = Seq(Event.forRichEvent(defaultLiveEvent)))

      asJsonString(response) must /("events") /# 0 / ("mainImageUrl" -> "https://media.guim.co.uk/b7c830bff5104b9ce9951928238fb09004d50335/0_0_1280_768/master/1280.jpg")
    }
  }

  def asJsonString(response: EventsResponse): String = Json.stringify(Json.toJson(response))
}

trait EventFixtures {
  def liveEvent(venue: EBVenue = EBVenue(None, None)) = GuLiveEvent(
    event = EBEvent(
      name = EBRichText(text = "The ten (food) commandments with Jay Rayner", html = "The ten (food) commandments with Jay Rayner"),
      description = None,
      url = "https://membership.theguardian.com/event/guardian-live-the-ten-food-commandments-with-jay-rayner-22576715564",
      id = "22576715564",
      start = DateTime.now(),
      end = DateTime.now().plusMonths(1),
      created = Instant.now(),
      venue = venue,
      capacity = 200,
      ticket_classes = Seq.empty,
      status = "live"),
    image = Option(GridImage(
      assets = List.empty,
      metadata = Grid.Metadata(None, None, None),
      master = Some(Asset(
        "http://media.guim.co.uk/b7c830bff5104b9ce9951928238fb09004d50335/0_0_1280_768/master/1280.jpg",
        Some("https://media.guim.co.uk/b7c830bff5104b9ce9951928238fb09004d50335/0_0_1280_768/master/1280.jpg"),
        Dimensions(1280, 768)
      ))
    )),
    contentOpt = None
  )

  val defaultLiveEvent = liveEvent()
}
