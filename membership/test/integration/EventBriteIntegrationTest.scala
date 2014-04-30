package integration

import play.api.test.PlaySpecification
import services.EventBriteService


class EventBriteIntegrationTest extends PlaySpecification {


  "EventBriteService" should {
    "return all events in the guardian account" in {
      val service = new EventBriteService with TestEventBriteAccount
      val events = await(service.getAllEvents())
      events.size mustEqual 4
    }
  }


}

trait TestEventBriteAccount {

  val eventUrl: String = "https://www.eventbriteapi.com/v3/users/99154249965/owned_events"
  val token: (String, String) = ("token" -> "***REMOVED***")

}
