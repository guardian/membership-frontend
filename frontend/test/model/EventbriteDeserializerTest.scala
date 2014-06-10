package model

import play.api.libs.json._
import play.api.test.PlaySpecification
import scala.io.Source
import model.Eventbrite._

class EventbriteDeserializerTest extends PlaySpecification {

  "EventbriteDeserializer" should {

    "should deserialize event json" in {
      import EventbriteDeserializer._ // class under test

      val event = Source.fromURL(this.getClass.getClassLoader.getResource("event-1.json")).mkString
      val ebResponse = Json.parse(event).as[EBResponse[EBEvent]]

      ebResponse.data.head.name.text === "Chris' big time jamboree"
    }
  }

}
