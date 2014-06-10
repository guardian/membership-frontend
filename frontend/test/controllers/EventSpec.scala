package controllers

import play.api.test.{ FakeRequest, WithApplication, PlaySpecification }
import services.{MemberService, EventbriteService}
import configuration.Config

object EventSpec extends PlaySpecification with Event {

  val eventService: EventbriteService = SandboxEventbriteService
  val memberService = MemberService

  "Event Index page" should {

    "display a single event" in new WithApplication {
      val result = details("11582434373")(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("Creative writing weekend with MJ Hyland")
      contentAsString(result) must contain("11582434373")
    }

  }
}

object SandboxEventbriteService extends EventbriteService {
  val apiUrl = Config.eventbriteApiUrl
  val apiToken = Config.eventbriteApiToken
  val apiEventListUrl = Config.eventbriteApiEventListUrl
}