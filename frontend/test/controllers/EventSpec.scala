package controllers

import play.api.test.{ FakeRequest, WithApplication, PlaySpecification }
import services.EventbriteService

object EventSpec extends PlaySpecification with Event {

  val eventService: EventbriteService = SandboxEventbriteService

  "Event Index page" should {

    "display list of all events" in new WithApplication {
      val result = list(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("Creative writing weekend with MJ Hyland")
      contentAsString(result) must contain("Advanced writing: Manuscript surgery")
    }

    "display a single event" in new WithApplication {
      val result = details("11582434373")(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("Creative writing weekend with MJ Hyland")
      contentAsString(result) must contain("11582434373")
    }

  }
}

object SandboxEventbriteService extends EventbriteService {
  override val eventListUrl: String = "https://www.eventbriteapi.com/v3/users/101063397097/owned_events"
  override val eventUrl: String = "https://www.eventbriteapi.com/v3/events/"
  override val token: (String, String) = ("token", "***REMOVED***")
}