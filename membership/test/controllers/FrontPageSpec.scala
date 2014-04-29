package controllers

import play.api.test.{FakeRequest, WithApplication, PlaySpecification}
import scala.concurrent._
import ExecutionContext.Implicits.global
import services.EventService
import model._
import model.EBTime
import model.EBRichText
import model.EBEvent
import model.EBVenue

object FrontPageSpec extends PlaySpecification with FrontPage {

  override val eventService: EventService = MockEventBriteService

  "Event Index page" should {

    "display list of all events" in new WithApplication {
      val result = index()(FakeRequest())

      status(result) must equalTo(OK)
      contentAsString(result) must contain("Event 1")
      contentAsString(result) must contain("Event 2")
    }

  }
}


object MockEventBriteService extends EventService {
  override def getAllEvents(): Future[Seq[EBEvent]] = {
    future {
      val name = EBRichText("Event 1", "")
      val name2 = EBRichText("Event 2", "")
      val dummyTime = new EBTime("", "", "")
      val dummyText = new EBRichText("dummy", "dummy")
      val dummyAddress = new EBAddress(None, None, None, None, None)
      val dummyVenue = new EBVenue(None, dummyAddress, None, None, None)
      List(EBEvent(name, dummyText, "", "", dummyTime, dummyTime, dummyVenue ), EBEvent(name2, dummyText, "", "", dummyTime, dummyTime, dummyVenue ))
    }
  }

}


