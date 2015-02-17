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
  val soldOutEvent = ebResponse.data.find(_.id == "12238163677").get
  val startedEvent = ebResponse.data.find(_.id == "12972720757").get

  val ticketedEvent = ebLiveEvent;

  "event" should {
    "be sold out" in {
      soldOutEvent.isSoldOut mustEqual(true)
    }
    "not be sold out" in {
      ebLiveEvent.isSoldOut mustEqual(false)
    }
    "be bookable" in {
      ebLiveEvent.isBookable mustEqual(true)
    }
    "not be bookable when sold out" in {
      soldOutEvent.isBookable mustEqual(false)
    }
    "should display sold out text" in {
      soldOutEvent.statusText mustEqual("Sold out")
    }
    "should display draft status in event" in {
      ebDraftEvent.statusText mustEqual("Preview of Draft Event")
    }
    "should not display event status text" in {
      ebLiveEvent.statusText mustEqual("")
    }
    "not be bookable when it has started" in {
      startedEvent.isBookable mustEqual(false)
    }
    "should display past event text" in {
      startedEvent.statusText mustEqual("Past event")
      ebCompletedEvent.statusText mustEqual("Past event")
    }
    "not be bookable when it is in draft mode" in {
      ebDraftEvent.isBookable mustEqual(false)
    }
    "should return media service url if present" in {
      ebLiveEvent.mainImageUrl mustEqual Some("https://some-media-tool.co.uk/images/sdf8u8sdf898hnsdcvs89dc?crop=0_3_480_288")

    }
    "should not return media service url is missing" in {
      nonTicketedEvent.mainImageUrl must beNone
    }
  }

  "internal ticketing (tickets sold by us thru eventbrite, rather than external partners)" should {
    "be present when comment <!-- noTicketEvent --> is NOT in description" in {
      ticketedEvent.internalTicketing must beSome
    }
    "be none when comment <!-- noTicketEvent --> IS present in description" in {
      nonTicketedEvent.internalTicketing must beNone
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

  "Venue addressLine" should {
    "be a pleasantly formatted concatenation of the venue address" in {
      ebCompletedEvent.venue.addressLine mustEqual Some("Kings Place, 90 York Way, London, N1 9GU")
    }
  }

  "Venue google link" should {
    "include the uri encoded venue name and address as the query parameter" in {
      ebCompletedEvent.venue.googleMapsLink mustEqual Some("https://maps.google.com/?q=The%20Guardian,%20Kings%20Place,%2090%20York%20Way,%20London,%20N1%209GU")
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

  "description" should {
    "not contain a link back to masterclasses if it exists" in {
      val event = Resource.getJson("model/eventbrite/event-with-link.json").as[EBEvent]
      val link = "<A HREF=\"http://www.theguardian.com/guardian-masterclasses/data-visualisation\" REL=\"nofollow\">Full course and returns information on the Masterclasses website</A>"

      event.description.get.html must contain(link)
      event.description.get.cleanHtml must not contain link
    }

    "not strip other URLs" in {
      val desc = EBRichText("", "This is some text, go <a href=\"link\">here</a>")
      desc.html mustEqual desc.cleanHtml
    }

    "not remove the link if it is incorrectly formatted" in {
      val desc = EBRichText("", "<a href=\"blah\">Full course and returns information on</a>")
      desc.cleanHtml must contain("<a href=\"blah\">Full course and returns information on</a>")
    }
  }

  "providerOpt" should {
    "be empty when there is no provider" in {
      ebLiveEvent.providerOpt must beNone
    }

    "be some when there is a valid provider" in {
      val event = Resource.getJson("model/eventbrite/event-with-provider.json").as[EBEvent]
      event.providerOpt must beSome("birkbeck")
    }

    "be none when there is an invalid provider" in {
      val event = Resource.getJson("model/eventbrite/event-with-invalid-provider.json").as[EBEvent]
      event.providerOpt must beNone
    }
  }

  "memberDiscountOpt" should {
    "return Some when there is a general release ticket AND a discount-benefit ticket" in {
      val event = Resource.getJson("model/eventbrite/event-guardian-members-discount.json").as[EBEvent]
      val ticketing = event.internalTicketing.get

      ticketing.memberDiscountOpt must beSome
      ticketing.memberBenefitTickets.map(_.id) mustEqual Seq("32044312")
    }

    "return None when there is only a general release ticket" in {
      val event = Resource.getJson("model/eventbrite/event-standard-ticket-classes.json").as[EBEvent]
      val ticketing = event.internalTicketing.get

      ticketing.memberDiscountOpt must beNone
      ticketing.memberBenefitTickets.map(_.id) must beEmpty
    }
  }

  "slugToId" should {
    "return the id for a normal slug" in {
      EBEvent.slugToId("this-is-a-slug-1234") must beSome("1234")
    }

    "return a standalone id not in a slug" in {
      EBEvent.slugToId("1234") must beSome("1234")
    }
  }

  "isFree" should {
    "report event as a free event" in {
      val event = Resource.getJson("model/eventbrite/event-free-ticket-classes.json").as[EBEvent]

      event.internalTicketing.get.isFree must beTrue
    }
  }

  "isSoldOut" should {
    "report event as sold out event" in {
      val event = Resource.getJson("model/eventbrite/event-sold-out-ticket-classes.json").as[EBEvent]

      event.isSoldOut must beTrue
    }
  }
}
