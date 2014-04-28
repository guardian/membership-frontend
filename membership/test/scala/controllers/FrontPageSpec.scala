package controllers

import play.api.test.{FakeRequest, WithApplication, PlaySpecification}
import scala.concurrent._
import ExecutionContext.Implicits.global
import model.{DefaultMembershipEvent, MembershipEvent}
import services.EventService

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
  override def getEvents(): Future[Seq[MembershipEvent]] = {
    future {
      List(DefaultMembershipEvent("1", "Event 1"), DefaultMembershipEvent("2", "Event 2"))
    }
  }

}


