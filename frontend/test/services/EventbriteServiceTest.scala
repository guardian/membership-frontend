package services

import model.EventbriteTestObjects.TestRichEvent
import play.api.test.PlaySpecification
import model.Eventbrite.{EBEvent, EBError, EBObject}
import model.RichEvent.RichEvent
import scala.concurrent.Future
import play.api.libs.json.Reads
import utils.Resource
import monitoring.EventbriteMetrics
import model.EventGroup

class EventbriteServiceTest extends PlaySpecification {

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
    override def getFeaturedEvents: Seq[RichEvent] = Nil
    override def getTaggedEvents(tag: String): Seq[RichEvent] = Nil
    override def getPartnerEvents: Option[EventGroup] = None

    def mkRichEvent(event: EBEvent): Future[RichEvent] = Future.successful(TestRichEvent(event))
  }

  object TestEventbriteService {
    def apply[T](block: TestEventbriteService => T) = block(new TestEventbriteService)
  }

}
