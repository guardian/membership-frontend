package model

import model.EventbriteTestObjects._
import model.Eventbrite._
import model.Eventbrite.EBEventStatus._
import play.api.test.PlaySpecification
import org.joda.time.Instant

class EBEventTest extends PlaySpecification {

  val quantitySold = Some(10)
  val ebTicketsQuantitySoldTen = new EBTickets(None, None, false, None, quantitySold, None, None, None)
  val ebTicketsStartInPast     = new EBTickets(None, None, false, None, quantitySold, None, None, Some(Instant.now.plus(1000)))
  val ebTicketsStartInFuture   = new EBTickets(None, None, false, None, quantitySold, None, None, Some(Instant.now.minus(1000)))

  val ebCompletedEvent = new EBEvent(eventName("Completed Event"), None, None, "", "", eventTime, eventTime, eventVenue, None, Seq.empty, Some("completed"))
  val ebCanceledEvent = new EBEvent(eventName("Canceled Event"), None, None, "", "", eventTime, eventTime, eventVenue, None, Seq.empty, Some("canceled"))
  val ebSoldOutEvent = new EBEvent(eventName("Sold Out Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(1), Seq(ebTicketsQuantitySoldTen), Some("live"))
  val ebLiveEvent = new EBEvent(eventName("Live Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(20), Seq(ebTicketsStartInFuture), Some("live"))
  val ebPreLiveEvent = new EBEvent(eventName("Pre Live Event"), None, None, "", "", eventTime, eventTime, eventVenue, Some(20), Seq(ebTicketsStartInPast), Some("live"))
  val freeTicket = EBTickets(None, None, true, None, None, None, None, None)
  val expensivePricing = EBPricing("GBP", "\u00a31234.25", 123425)

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

  "getPrice" should {
    "be pleasantly formatted" in {
      expensivePricing.formattedPrice mustEqual("£1234")
    }
  }
}
