package model

import model.RichEvent.GuLiveEvent
import play.api.test.PlaySpecification
import play.api.libs.json.{Json}
import model.EventbriteTestObjects._

class SchemaTest extends PlaySpecification {

  "EventSchema" should {

    "generate JSON-LD schema data for an event" in {

      val event = GuLiveEvent(eventWithName("Test Event"), None, None, None)
      val json = Json.toJson(event.schema)

      (json \ "@context").as[String] === "http://schema.org"
      (json \ "@type").as[String] === "Event"
      (json \ "name").as[String] === "Test Event"
      (json \ "description").as[String] === "Event Description"
    }

  }

}
