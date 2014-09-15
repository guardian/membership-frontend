package model

import model.EventbriteTestObjects._
import model.Eventbrite._
import model.Eventbrite.EBEventStatus._
import play.api.test.PlaySpecification
import org.joda.time.Instant
import utils.Resource

class EBEventTest extends PlaySpecification {

  val quantitySold = Some(10)
  val ebTicketsQuantitySoldTen = new EBTickets(None, None, false, None, quantitySold, None, None, None, Some(true))
  val ebTicketsStartInPast     = new EBTickets(None, None, false, None, quantitySold, None, None, Some(Instant.now.plus(1000)), Some(true))
  val ebTicketsStartInFuture   = new EBTickets(None, None, false, None, quantitySold, None, None, Some(Instant.now.minus(1000)), Some(true))

  val ebCompletedEvent = new EBEvent(eventName("Completed Event"), None, None, "", "", eventTime, eventTime, eventVenue, None, Seq.empty, Some("completed"))
  val ebCanceledEvent = new EBEvent(eventName("Canceled Event"), None, None, "", "", eventTime, eventTime, eventVenue, None, Seq.empty, Some("canceled"))
  val ebSoldOutEvent = new EBEvent(eventName("Sold Out Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(1), Seq(ebTicketsQuantitySoldTen), Some("live"))
  val ebLiveEvent = new EBEvent(eventName("Live Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(20), Seq(ebTicketsStartInFuture), Some("live"))
  val ebPreLiveEvent = new EBEvent(eventName("Pre Live Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(20), Seq(ebTicketsStartInPast), Some("live"))
  val freeTicket = EBTickets(None, None, true, None, None, None, None, None, Some(true))

  "getStatus" should {
    "be Completed" in {
      ebCompletedEvent.getStatus mustEqual(Completed)
    }
    "be Cancelled" in {
      ebCanceledEvent.getStatus mustEqual(Cancelled)
    }
    "be SoldOut" in {
      ebSoldOutEvent.getStatus mustEqual(SoldOut)
    }
    "be Live" in {
      ebLiveEvent.getStatus mustEqual(Live)
    }
    "be PreLive" in {
      ebPreLiveEvent.getStatus mustEqual(PreLive)
    }
    "be PreLive when status=draft" in {
      import EventbriteDeserializer._ // class under test

      val event = Resource.getJson("model/eventbrite/events.12016047321.json").as[EBEvent]

      event.getStatus === PreLive
    }
  }

  "getPrice" should {
    "be pleasantly formatted with pence if the value is not whole pounds" in {
      EBPricing("GBP", "\u00a31234.25", 123425).formattedPrice mustEqual("£1234.25")
    }
    "be pleasantly formatted as whole pounds if there are no pence" in {
      EBPricing("GBP", "\u00a31234.00", 123400).formattedPrice mustEqual("£1234")
    }
  }
}
