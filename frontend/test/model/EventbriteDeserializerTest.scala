package model

import play.api.libs.json._
import play.api.test.PlaySpecification
import scala.io.Source
import model.Eventbrite.EBResponse

class EventbriteDeserializerTest extends PlaySpecification {

  "EventbriteDeserializer" should {

    "should deserialize event json" in {
      import EventbriteDeserializer._ // class under test

      val event = Source.fromURL(this.getClass.getClassLoader.getResource("event-1.json")).mkString
      val ebResponse = Json.parse(event).as[EBResponse]

      ebResponse.events.head.name.text === "Chris' big time jamboree"
    }
  }

}
