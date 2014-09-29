package model

import model.Eventbrite._
import play.api.test.PlaySpecification
import org.joda.time.{ Instant, DateTime }
import utils.Resource
import EventbriteDeserializer._

class EBEventTest extends PlaySpecification {

  val event = Resource.getJson("model/eventbrite/events.2014-26-09.dev.account.json")
  val ebResponse = event.as[EBResponse[EBEvent]]
  val ebCompletedEvent = ebResponse.data.find(_.id == "11582434373").get
  val ebStartedEvent = ebResponse.data.find(_.id == "11585910771").get
  val ebEndedEvent = ebResponse.data.find(_.id == "11583186623").get
  val ebCancelledEvent = ebResponse.data.find(_.id == "11582307995").get
  val ebSoldOutEvent = ebResponse.data.find(_.id == "11582929855").get
  val ebLiveEvent = ebResponse.data.find(_.id == "11582592847").get
  val ebLiveEventTicketsNotOnSale = ebResponse.data.find(_.id == "11583080305").get
  val ebDraftEvent = ebResponse.data.find(_.id == "11583116413").get


 "getStatus on event " should {
    "that has a status='completed' should return Completed" in {
      ebCompletedEvent.getStatus mustEqual(Completed)
    }

    "that has a status='started' should return Completed" in {
      ebStartedEvent.getStatus mustEqual(Completed)
    }

    "that has a status='ended' should return Completed" in {
      ebEndedEvent.getStatus mustEqual(Completed)
    }

    "that has a status='cancelled' should return Cancelled" in {
      ebCancelledEvent.getStatus mustEqual(Cancelled)
    }

    "that has a status='live' with ticket quantity sold equalling quantity total should return SoldOut" in {
      ebSoldOutEvent.getStatus mustEqual(SoldOut)
    }
    "that has a status='live' should return Live" in {
      ebLiveEvent.getStatus mustEqual(Live)
    }
   "that has a status='live' but tickets are not yet on sale should return PreLive" in {
     ebLiveEventTicketsNotOnSale.getStatus mustEqual(PreLive)
    }
    "that has a status='draft' should return Draft" in {
      ebDraftEvent.getStatus mustEqual(Draft)
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
