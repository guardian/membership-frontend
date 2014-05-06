package controllers

import play.api.test.{FakeRequest, WithApplication, PlaySpecification}
import scala.concurrent._
import ExecutionContext.Implicits.global
import services.EventService
import model.EBEvent
import model.EventbriteTestObjects._



object FrontPageSpec extends PlaySpecification with EventController {

  val eventService: EventService = MockEventbriteService

  "Event Index page" should {

    "display list of all events" in new WithApplication {
      val result = renderEventsIndex(FakeRequest())
      status(result) must equalTo(OK)
      contentAsString(result) must contain("Event 1")
      contentAsString(result) must contain("Event 2")
    }

  }
}


object MockEventbriteService extends EventService {
   def getAllEvents(): Future[Seq[EBEvent]] = {
    future {
      List(eventWithName("Event 1"), eventWithName("Event 2"))
    }
  }
}




