package model

import model.Eventbrite._
import play.api.test.PlaySpecification
import org.joda.time.{ Instant, DateTime }
import utils.Resource
import EventbriteDeserializer._

class EBEventTest extends PlaySpecification {

  val event = Resource.getJson("model/eventbrite/owned-events.2014-10-24.PROD.page-1.json")
  val ebResponse = event.as[EBResponse[EBEvent]]
  val ebCompletedEvent = ebResponse.data.find(_.id == "13125971133").get
  val ebCancelledEvent = ebResponse.data.find(_.id == "13087550215").get
  // val ebSoldOutEvent = ebResponse.data.find(_.id == "13043179501").get
  val ebLiveEvent = ebResponse.data.find(_.id == "12104040511").get
  // val ebLiveEventTicketsNotOnSale = ebResponse.data.find(_.id == "11583080305").get
  val ebDraftEvent = ebResponse.data.find(_.id == "13607066101").get
  val nonTicketedEvent = ebResponse.data.find(_.id == "13602460325").get
  val ticketedEvent = ebLiveEvent;

 "getStatus on event " should {
    "that has a status='completed' should return Completed" in {
      ebCompletedEvent.getStatus mustEqual(Completed)
    }

    "that has a status='cancelled' should return Cancelled" in {
      ebCancelledEvent.getStatus mustEqual(Cancelled)
    }

//    "that has a status='live' with ticket quantity sold equalling quantity total should return SoldOut" in {
//      ebSoldOutEvent.getStatus mustEqual(SoldOut)
//    }
    "that has a status='live' should return Live" in {
      ebLiveEvent.getStatus mustEqual(Live)
    }
//   "that has a status='live' but tickets are not yet on sale should return PreLive" in {
//     ebLiveEventTicketsNotOnSale.getStatus mustEqual(PreLive)
//    }
    "that has a status='draft' should return Draft" in {
      ebDraftEvent.getStatus mustEqual(Draft)
    }
  }

  "isNoTicketEvent on event " should {
    "should return true when comment <!-- noTicketEvent --> is present in description" in {
      nonTicketedEvent.isNoTicketEvent mustEqual(true)
    }
    "should return false when comment <!-- noTicketEvent --> is NOT in description" in {
      ticketedEvent.isNoTicketEvent mustEqual(false)
    }
  }

  "getPrice" should {
    "be pleasantly formatted with pence if the value is not whole pounds" in {
      EBPricing(123425).formattedPrice mustEqual("£1234.25")
    }
    "be pleasantly formatted as whole pounds if there are no pence" in {
      EBPricing(123400).formattedPrice mustEqual("£1234")
    }
  }

  "Event location" should {
    "be correct with all fields present" in {
      val location = ebCompletedEvent.venue.address.get
      location.address_1.get mustEqual("Kings Place")
      location.address_2.get mustEqual("90 York Way")
      location.city.get mustEqual("London")
      location.region.getOrElse("") mustEqual("")
      location.postal_code.get mustEqual("N1 9GU")
      location.country.get mustEqual("GB")
    }
  }
}
