package services
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.parser.JsonParser
import com.gu.i18n.Currency.GBP
import com.gu.memsub.Subscription.{ProductRatePlanId, RatePlanId}
import com.gu.memsub._
import com.gu.memsub.services
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2._
import com.gu.salesforce._
import model.Eventbrite.EBAccessCode
import model.EventbriteTestObjects._
import model.{ContentDestination, EventDestination}
import org.joda.time.LocalDate
import org.specs2.mutable.Specification
import play.api.mvc.Session
import utils.Resource

import scalaz.Id._

class DestinationServiceTest extends Specification {

  "DestinationService" should {

    val destinationService = new DestinationService[Id](
      getBookableEvent = _ => Some(TestRichEvent(eventWithName().copy(id = "0123456"))),
      capiItemQuery = _ => JsonParser.parseItemThrift(Resource.get("model/content.api/item.json")),
      createCode = (_, _) => Some(EBAccessCode("some-discount-code", 2))
    )

    def createRequestWithSession(newSessions: (String, String)*) = {

      val testMember = Contact("id", None, None, Some("fn"), "ln", Some("email"), new DateTime(), "contactId", "accountId", None, None, None, None, None)
      val partnerCharge: PaidCharge[com.gu.memsub.Partner.type, Month] = PaidCharge[com.gu.memsub.Partner.type, Month](com.gu.memsub.Partner, Month(), PricingSummary(Map(GBP -> Price(0.1f, GBP))))
      val testSub: Subscription[SubscriptionPlan.Member] = new Subscription[SubscriptionPlan.Partner](
        id = com.gu.memsub.Subscription.Id(""),
        name = com.gu.memsub.Subscription.Name(""),
        accountId = com.gu.memsub.Subscription.AccountId(""),
        startDate = new LocalDate("2015-01-01"),
        termStartDate = new LocalDate("2015-01-01"),
        firstPaymentDate = new LocalDate("2015-01-01"),
        termEndDate = new LocalDate("2016-01-01"),
        promoCode = None,
        casActivationDate = None,
        isCancelled = false,
        plans = scalaz.NonEmptyList(new PaidSubscriptionPlan[Product.Membership, PaidCharge[com.gu.memsub.Partner.type, Month]](
          id = RatePlanId(""), productRatePlanId = ProductRatePlanId(""), name = "name", product = Product.Membership, description = "", features = Nil,
          charges = partnerCharge,
          chargedThrough = None, start = new LocalDate("2015-01-01"), end = new LocalDate("2016-01-01"), productName = "")),
        hasPendingFreePlan = false,
        readerType = ReaderType.Direct
      )

      val testSubscriber: Subscriber.Member = Subscriber(testSub, testMember)
      (Session(newSessions.toMap), testSubscriber)
    }

    "should return a content destination url if join-referrer is in the request session" in {
      val (request, _) = createRequestWithSession("join-referrer" -> "http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts")
      val result = destinationService.contentDestinationFor(request)
      result.get.item.content.id mustEqual "membership/2015/apr/17/guardian-live-diversity-in-the-arts"
    }

    "should return an event destination url if preJoinReturnUrl is in the request session" in {
      val (request, member) = createRequestWithSession("preJoinReturnUrl" -> "/event/0123456/buy")
      val result = destinationService.eventDestinationFor(request, member)
      result.get.event.id mustEqual "0123456"
    }

    "should return either content or event destination if both are supplied" in {
      val (request, member) = createRequestWithSession("join-referrer" -> "http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts", "preJoinReturnUrl" -> "/event/0123456/buy")

      destinationService.returnDestinationFor(request, member).get match {
        case EventDestination(event, _) => event.id mustEqual "0123456"
        case ContentDestination(item) => item.content.id mustEqual "membership/2015/apr/17/guardian-live-diversity-in-the-arts"
      }
    }
  }
}
