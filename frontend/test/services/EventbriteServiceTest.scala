package services

import model.EventbriteTestObjects
import play.api.test.PlaySpecification
import model.Eventbrite.{EBEvent, EBError, EBObject}
import model.RichEvent.RichEvent
import scala.concurrent.{Await, Future}
import play.api.libs.json.Reads
import utils.Resource
import scala.concurrent.duration._
import monitoring.EventbriteMetrics

class EventbriteServiceTest extends PlaySpecification {

  def testEvent = TestRichEvent(EventbriteTestObjects.eventWithName("test"))

  "EventbriteService" should {

    "reuses an existing discount code" in TestEventbriteService { service =>
      Await.ready(service.createOrGetDiscount(testEvent, "5ZCYERL5"), 5.seconds)
      service.lastRequest mustEqual RequestInfo.empty
    }

    "creates a new discount code" in TestEventbriteService { service =>
      Await.ready(service.createOrGetDiscount(testEvent, "NEW"), 5.seconds)

      service.lastRequest mustEqual RequestInfo(
        url = s"http://localhost:9999/v1/events/test/discounts",
        body = Map(
          "discount.code" -> Seq("NEW"),
          "discount.quantity_available" -> Seq("2"),
          "discount.percent_off" -> Seq("20")
        )
      )
    }
  }

  case class TestRichEvent(event: EBEvent) extends RichEvent {
    val imgUrl = ""
    val socialImgUrl = ""
    val tags = Nil
  }


  class TestEventbriteService extends EventbriteService {
    val apiToken = ""
    val maxDiscountQuantityAvailable = 2
    val apiURL = "http://localhost:9999/v1"
    val apiEventListUrl = "events"

    var lastRequest = RequestInfo.empty

    val wsMetrics = new EventbriteMetrics("test")

    override def get[A <: EBObject](endpoint: String, params: (String, String)*)(implicit reads: Reads[A], error: Reads[EBError]): Future[A] = {
      endpoint match {
        case "events/test/discounts" =>
          val resource = Resource.getJson(s"model/eventbrite/discounts.json")
          Future.successful(resource.as[A])
        case _ =>
          lastRequest = RequestInfo(s"$apiURL/$endpoint", Map.empty)
          Future.failed[A](EBError("internal", "Not implemented", 500)) // don't care
      }
    }

    override def post[A <: EBObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A], error: Reads[EBError]): Future[A] = {
      lastRequest = RequestInfo(s"$apiURL/$endpoint", data)
      Future.failed[A](EBError("internal", "Not implemented", 500)) // don't care
    }

    override def events: Seq[RichEvent] = Nil
    override def eventsArchive: Seq[RichEvent] = Nil
    override def priorityEventOrdering: Seq[String] = Nil
    def mkRichEvent(event: EBEvent): RichEvent = TestRichEvent(event)
  }

  object TestEventbriteService {
    def apply[T](block: TestEventbriteService => T) = block(new TestEventbriteService)
  }

}
