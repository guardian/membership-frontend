package model

import model.EventbriteTestObjects._
import model.EBEventStatus._
import play.api.test.PlaySpecification
import org.joda.time.Instant

class EBEventTest extends PlaySpecification {

  val quantitySold = Some(10)
  val ebTicketsQuantitySoldTen = new EBTickets(None, None, None, None, quantitySold, None, None, None)
  val ebTicketsStartInPast     = new EBTickets(None, None, None, None, quantitySold, None, None, Some(Instant.now.plus(1000)))
  val ebTicketsStartInFuture   = new EBTickets(None, None, None, None, quantitySold, None, None, Some(Instant.now.minus(1000)))

  val ebCompletedEvent = new EBEvent(eventName("Completed Event"), None, None, "", "", eventTime, eventTime, eventVenue, None, None, Some("completed"))
  val ebCanceledEvent = new EBEvent(eventName("Canceled Event"), None, None, "", "", eventTime, eventTime, eventVenue, None, None, Some("canceled"))
  val ebSoldOutEvent = new EBEvent(eventName("Sold Out Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(1), Some(Seq(ebTicketsQuantitySoldTen)), Some("live"))
  val ebLiveEvent = new EBEvent(eventName("Live Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(20), Some(Seq(ebTicketsStartInFuture)), Some("live"))
  val ebPreLiveEvent = new EBEvent(eventName("Pre Live Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(20), Some(Seq(ebTicketsStartInPast)), Some("live"))

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

  }
}
