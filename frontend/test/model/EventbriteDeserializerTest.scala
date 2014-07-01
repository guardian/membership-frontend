package model

import play.api.test.PlaySpecification
import model.Eventbrite._
import utils.Resource

class EventbriteDeserializerTest extends PlaySpecification {

  "EventbriteDeserializer" should {

    "should deserialize event json" in {
      import EventbriteDeserializer._ // class under test

      val event = Resource.getJson("model/eventbrite/events.json")
      val ebResponse = event.as[EBResponse[EBEvent]]

      ebResponse.data.head.name.text === "Chris' big time jamboree"
    }
  }

}
