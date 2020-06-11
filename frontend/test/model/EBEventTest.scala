package model

import io.lemonlabs.uri.Uri
import model.Eventbrite._
import model.EventbriteDeserializer._
import org.joda.time.Instant
import play.api.test.PlaySpecification
import utils.Resource
import utils.Implicits._

object EBEventTest {
  val event = Resource.getJson("model/eventbrite/owned-events.2014-10-24.PROD.page-1.json")
  val ebResponse = event.as[EBResponse[EBEvent]]
}

class EBEventTest extends PlaySpecification {
  import EBEventTest.ebResponse
  // val ebSoldOutEvent = ebResponse.data.find(_.id == "13043179501").get
  // val ebLiveEventTicketsNotOnSale = ebResponse.data.find(_.id == "11583080305").get
  def ebLiveEvent = ebResponse.data.find(_.id == "12104040511").get.toAssumedEventWithDescription
  def ebDraftEvent = ebResponse.data.find(_.id == "13607066101").get.toAssumedEventWithDescription
  def ebCompletedEvent = ebResponse.data.find(_.id == "13125971133").get.toAssumedEventWithDescription
  def ebCancelledEvent = ebResponse.data.find(_.id == "13087550215").get.toAssumedEventWithDescription
  def nonTicketedEvent = ebResponse.data.find(_.id == "13602460325").get.toAssumedEventWithDescription
  def soldOutEvent = ebResponse.data.find(_.id == "12238163677").get.toAssumedEventWithDescription
  def startedEvent = ebResponse.data.find(_.id == "12972720757").get.toAssumedEventWithDescription
  def completedEvent = ebResponse.data.find(_.id == "13024577863").get.toAssumedEventWithDescription
  def limitedAvailabilityEvent = ebResponse.data.find(_.id == "12718560557").get.toAssumedEventWithDescription

  def ticketedEvent = ebLiveEvent

  def updateEventWithDescription(event: EventWithDescription, desc: String) = event.copy(ebDescription = event.ebDescription.copy(description = desc))

  "structured event" should {

    val structured = {
      val event = Resource.getJson("model/eventbrite/structuredEvent/event.json").as[EBEvent]
      val desc = Resource.getJson("model/eventbrite/structuredEvent/description.json").as[EBDescription]
      EventWithDescription(event, desc)
    }

    "have the summary in the event description and the main description separately" in {
      val expectedDesc = s"""<div>A Guardian Live event with award-winning Turkish novelist Elif Shafak.</div>${"\n"}<div style=\"margin-top: 20px\"><div><div style=\"margin:20px 10px;font-size:15px;line-height:22px;font-weight:400;text-align:left;\"><p>Elif Shafak, activist, essayist, academic and author of 17 books - including the Booker Prize shortlisted 10 Minutes 38 Seconds in This Strange World - will be joining us for an evening of conversation.</p><p>10 Minutes is her most recent novel, and follows the story of murdered sex worker Leila, who enters into a new state of self awareness in the minutes between her heart stopping and her brain activity dying. Like many of Shafak’s other books, the narrative transcends time and borders to offer an intricate, profound and often moving examination into the trauma women are subjected to at the hands of the patriarchy.</p><p>As well as writing fiction, Shafak writes and speaks about international politics, far-right populism, pluralism, women’s rights and democracy. She is an outspoken critic of the Turkish government, and last year her novels became a target for “crimes of obscenity”; fearing arrest, Shafak has since been living in self-imposed exile in London.</p><p>She will be joining us to talk about her career as a writer, as well as her activism, the importance of free speech, and art as an antidote to political uncertainty.</p></div></div></div>"""
      structured.ebDescription.description mustEqual(expectedDesc)
      structured.ebEvent.description.map(_.html) mustEqual(Some("A Guardian Live event with award-winning Turkish novelist Elif Shafak."))
    }
  }

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
      soldOutEvent.statusText mustEqual Some("Sold out")
    }
    "not be limited availability when sold out" in {
      limitedAvailabilityEvent.isSoldOut mustEqual(false)
      soldOutEvent.isLimitedAvailability mustEqual(false)
    }
    "should be flagged as limited availability when limited tickets available" in {
      limitedAvailabilityEvent.isLimitedAvailability mustEqual(true)
    }
    "should display draft status in event" in {
      ebDraftEvent.statusText mustEqual Some("Preview of Draft Event")
    }
    "should not display event status text" in {
      ebLiveEvent.statusText mustEqual None
    }
    "not be bookable when it has started" in {
      ebLiveEvent.isBookable mustEqual(true)
      startedEvent.isBookable mustEqual(true)
      completedEvent.isBookable mustEqual(false)
    }
    "should display past event text" in {
      ebLiveEvent.statusText mustEqual None
      startedEvent.statusText mustEqual None
      ebCompletedEvent.statusText mustEqual Some("Past event")
    }
    "should handle multi-day events" in{
      val multiDayEvent = Resource.getJson("model/eventbrite/event-started-multi-day.json").as[EBEvent].toAssumedEventWithDescription
      multiDayEvent.statusText mustEqual None
      multiDayEvent.isBookable mustEqual(true)
    }
    "not be bookable when it is in draft mode" in {
      ebDraftEvent.isBookable mustEqual(false)
    }
    "should return media service url if present" in {
      ebLiveEvent.mainImageUrl mustEqual Some(Uri.parse("https://some-media-tool.co.uk/images/sdf8u8sdf898hnsdcvs89dc?crop=0_3_480_288"))
    }
    "should be tolerent in parsing, even if there are extraneous characters after the url" in {
      updateEventWithDescription(ebLiveEvent, "<!-- main-image: https://media.gutools.co.uk/images/e5b35b1f20588172b24960314cbf8e9c8482d3bf?crop=27_0_1800_1080 ! -->")
        .mainImageUrl must beSome(Uri.parse("https://media.gutools.co.uk/images/e5b35b1f20588172b24960314cbf8e9c8482d3bf?crop=27_0_1800_1080"))
    }
    "should not blow up even if the url is really bizarre" in {
      updateEventWithDescription(ebLiveEvent, "<!-- main-image: http://test.net/## -->").mainImageUrl must not(throwA[Exception])
    }
    "should not return media service url is missing" in {
      nonTicketedEvent.mainImageUrl must beNone
    }
    "should display venue description" in {
      ebLiveEvent.ebEvent.venue.addressDefaultLine.get mustEqual "The Royal Institution, London, W1S 4BS"
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

  "feeText" should {
    "include tax" in {
      val ticketClass = EBTicketClass(
        id = "",
        name = "",
        description = None,
        free = false,
        quantity_total = 1,
        quantity_sold = 0,
        on_sale_status = None,
        cost = None,
        fee = Some(EBPricing(600)),
        tax = Some(EBPricing(130)),
        sales_end = Instant.now(),
        sales_start = None,
        hidden = None
      )

      ticketClass.feeText must beSome("£7.30")
      ticketClass.copy(tax = None).feeText must beSome("£6")
      ticketClass.copy(fee = None, tax = None).feeText must beNone
    }
  }

  "Venue addressLine" should {
    "be a pleasantly formatted concatenation of the venue address" in {
      ebCompletedEvent.ebEvent.venue.addressLine mustEqual Some("Kings Place, 90 York Way, London, N1 9GU")
    }
  }

  "Venue google link" should {
    "include the uri encoded venue name and address as the query parameter" in {
      ebCompletedEvent.ebEvent.venue.googleMapsLink mustEqual Some("https://maps.google.com/?q=The%20Guardian,%20Kings%20Place,%2090%20York%20Way,%20London,%20N1%209GU")
    }
  }

  "Event location" should {
    "be correct with all fields present" in {
      val location = ebCompletedEvent.ebEvent.venue.address.get
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
      ebLiveEvent.ebDescription.providerOpt must beNone
    }

    "be some when there is a valid provider" in {
      val event = Resource.getJson("model/eventbrite/event-with-provider.json").as[EBEvent].toAssumedEventWithDescription.ebDescription
      event.providerOpt.map(_.id).get mustEqual "birkbeck"
      event.providerOpt.map(_.title).get mustEqual "Birkbeck"
    }

    "be none when there is an invalid provider" in {
      val event = Resource.getJson("model/eventbrite/event-with-invalid-provider.json").as[EBEvent].toAssumedEventWithDescription.ebDescription
      event.providerOpt must beNone
    }
  }

  "memberDiscountOpt" should {
    "return Some when there is a general release ticket AND a discount-benefit ticket" in {
      val event = Resource.getJson("model/eventbrite/event-guardian-members-discount.json").as[EBEvent].toAssumedEventWithDescription
      val ticketing = event.internalTicketing.get

      ticketing.memberDiscountOpt must beSome
      ticketing.memberBenefitTickets.map(_.id) mustEqual Seq("32044312")
    }

    "return None when there is only a general release ticket" in {
      val event = Resource.getJson("model/eventbrite/event-standard-ticket-classes.json").as[EBEvent].toAssumedEventWithDescription
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
      val event = Resource.getJson("model/eventbrite/event-free-ticket-classes.json").as[EBEvent].toAssumedEventWithDescription

      event.internalTicketing.get.isFree must beTrue
    }
  }

  "isSoldOut" should {
    "report event as sold out event" in {
      val event = Resource.getJson("model/eventbrite/event-sold-out-ticket-classes.json").as[EBEvent].toAssumedEventWithDescription

      event.isSoldOut must beTrue
    }
    "report an event with spare capacity as Sold Out if EventBrite says it is (due to people on waitlist)" in {
      val event = Resource.getJson("model/eventbrite/event.not-sold-out-but-populated-waitlist.json").as[EBEvent].toAssumedEventWithDescription

      event.isSoldOut must beTrue
    }
  }
}
