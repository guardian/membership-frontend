package integration

import play.api.test.PlaySpecification
import services.EventbriteService


class EventbriteIntegrationTest extends PlaySpecification {


  "EventbriteService" should {
    "return all events in the guardian account" in {
      val service = new EventbriteService with TestEventbriteAccount
      val events = await(service.getAllEvents())
      events.size mustEqual 4
    }
  }


}

trait TestEventbriteAccount {

  val eventUrl: String = "https://www.eventbriteapi.com/v3/users/99154249965/owned_events"
  val token: (String, String) = ("token" -> "***REMOVED***")

}
