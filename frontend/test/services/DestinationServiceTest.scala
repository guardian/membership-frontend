package services

import actions.{Subscriber, SubscriptionRequest}
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.parser.JsonParser
import com.gu.i18n.{Currency, GBP}
import com.gu.identity.play.{AccessCredentials, AuthenticatedIdUser, IdMinimalUser}
import com.gu.membership.PaidMembershipPlan
import com.gu.memsub.Subscription.{MembershipSub, ProductRatePlanId}
import com.gu.memsub._
import com.gu.salesforce.Tier.Partner
import com.gu.salesforce._
import model.Eventbrite.EBAccessCode
import model.EventbriteTestObjects._
import model.{ContentDestination, EventDestination}
import org.joda.time.LocalDate
import org.scalatest.concurrent.ScalaFutures
import org.specs2.mock.Mockito
import play.api.mvc.Security.AuthenticatedRequest
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification}
import play.api.{Application, GlobalSettings}
import utils.Resource

import scala.concurrent.Future

class DestinationServiceTest extends PlaySpecification with Mockito with ScalaFutures {

  "DestinationService" should {

    val fakeApplicationWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() {
      override def onStart(app: Application) {}
    }))

    val fakeMemberService = mock[MemberService]

    object DestinationServiceTest extends DestinationService {
      override val contentApiService = mock[GuardianContentService]
      override val eventbriteService = mock[EventbriteCollectiveServices]
      override def memberService(request: SubscriptionRequest[_]): MemberService = fakeMemberService
    }

    val destinationService = DestinationServiceTest

    def createRequestWithSession(newSessions: (String, String)*) = {

      val testMember = Contact("id", None, Some("fn"), "ln", "email", new DateTime(), "contactId", "accountId")
      val testSub: MembershipSub = new Subscription(
        id = Subscription.Id(""),
        name = Subscription.Name(""),
        accountId = Subscription.AccountId(""),
        currency = Currency.all.head,
        productRatePlanId = Subscription.ProductRatePlanId(""),
        productName = "productName",
        startDate = new LocalDate("2015-01-01"),
        termStartDate = new LocalDate("2015-01-01"),
        termEndDate = new LocalDate("2016-01-01"),
        features = Nil,
        promoCode = None,
        casActivationDate = None,
        isCancelled = false,
        ratePlanId = "",
        isPaid = true
      ) with PaidPS[PaidMembershipPlan[Status, PaidTier, BillingPeriod]] {

        override def recurringPrice: Price = new Price(0.1f, GBP)
        override def firstPaymentDate: LocalDate = new LocalDate("2015-01-01")
        override def chargedThroughDate: Option[LocalDate] = None
        override def priceAfterTrial: Price = recurringPrice
        override def hasPendingAmendment: Boolean = false
        override def plan = new PaidMembershipPlan[Current, Partner, Month](Current(), Tier.Partner(), Month(), ProductRatePlanId(""), PricingSummary(Map(GBP -> Price(0.1f, GBP))) )
      }

      val minimalUser: IdMinimalUser = IdMinimalUser("123", None)
      val fakeRequest = FakeRequest().withSession(newSessions: _*)
      val ar = new AuthenticatedRequest(AuthenticatedIdUser(AccessCredentials.Cookies("foo", "bar"), minimalUser), fakeRequest)

      new SubscriptionRequest(mock[TouchpointBackend],ar) with Subscriber {
        override def subscriber = Subscriber(testSub, testMember)
      }

    }

    "should return a content destination url if join-referrer is in the request session" in {
      running(fakeApplicationWithGlobal) {

        val request = createRequestWithSession("join-referrer" -> "http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts")

        //mock the Content API response
        val item = JsonParser.parseItem(Resource.get("model/content.api/item.json"))
        destinationService.contentApiService.contentItemQuery("/membership/2015/apr/17/guardian-live-diversity-in-the-arts") returns Future.successful(item)

        //call the method under test
        val futureResult = destinationService.contentDestinationFor(request)

        //verify eventDestinationFor returns a valid content destination
        whenReady(futureResult) { contentDestinationOpt =>
          contentDestinationOpt.get.item.content.id mustEqual "membership/2015/apr/17/guardian-live-diversity-in-the-arts"
        }
      }
    }

    "should return an event destination url if preJoinReturnUrl is in the request session" in {
      running(fakeApplicationWithGlobal) {

        val request = createRequestWithSession("preJoinReturnUrl" -> "/event/0123456/buy")


        //mock the Eventbrite response and discount creation
        val event = TestRichEvent(eventWithName().copy(id = "0123456"))
        destinationService.eventbriteService.getBookableEvent("0123456") returns Some(event)
        fakeMemberService.createEBCode(request.subscriber, event) returns Future.successful(Some(EBAccessCode("some-discount-code", 2)))

        //call the method under test
        val futureResult = destinationService.eventDestinationFor(request)

        //verify eventDestinationFor returns a valid event destination
        whenReady(futureResult) { eventDestinationOpt =>
          eventDestinationOpt.get.event.id mustEqual "0123456"
        }
      }
    }

    "should return either content or event destination if both are supplied" in {
      running(fakeApplicationWithGlobal) {

        val request = createRequestWithSession("join-referrer" -> "http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts", "preJoinReturnUrl" -> "/event/0123456/buy")

        //mock the Content API response
        val item = JsonParser.parseItem(Resource.get("model/content.api/item.json"))
        destinationService.contentApiService.contentItemQuery("/membership/2015/apr/17/guardian-live-diversity-in-the-arts") returns Future.successful(item)

        //mock the Eventbrite response and discount creation
        val event = TestRichEvent(eventWithName().copy(id = "0123456"))
        destinationService.eventbriteService.getBookableEvent("0123456") returns Some(event)
        fakeMemberService.createEBCode(request.subscriber, event) returns Future.successful(Some(EBAccessCode("some-discount-code", 2)))

        //call the method under test with the request
        val futureResult = destinationService.returnDestinationFor(request)

        //verify returnForDestination returns a valid destination
        whenReady(futureResult) { destinationOpt =>
          val destination = destinationOpt.get
          destination match {
            case eventDestination: EventDestination => eventDestination.event.id mustEqual "0123456"
            case contentDestination: ContentDestination => contentDestination.item.content.id mustEqual "membership/2015/apr/17/guardian-live-diversity-in-the-arts"
          }
          val validDestination = destination.isInstanceOf[ContentDestination] || destination.isInstanceOf[EventDestination]
          validDestination must_== true
        }
      }
    }
  }
}
