package controllers

import play.api.test.{ FakeRequest, WithApplication, PlaySpecification }
import services.EventbriteService

object EventControllerSpec extends PlaySpecification with Event {

  val eventService: EventbriteService = SandboxEventbriteService

  "Event Index page" should {

    "display list of all events" in new WithApplication {
      val result = renderEventsIndex(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("An evening with Chris Finch")
      contentAsString(result) must contain("Chris High Res Party time")
    }

    "display a single event" in new WithApplication {
      val result = renderEventPage("11464752383")(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("Chris High Res Party time")
      contentAsString(result) must contain("11464752383")
    }

  }
}

object SandboxEventbriteService extends EventbriteService {
  override val eventListUrl: String = "https://www.eventbriteapi.com/v3/users/99154249965/owned_events"
  override val eventUrl: String = "https://www.eventbriteapi.com/v3/events/"
  override val token: (String, String) = ("token", "***REMOVED***")
}