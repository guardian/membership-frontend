package services

import actions.AnyMemberTierRequest
import com.gu.contentapi.client.parser.JsonParser
import com.gu.membership.salesforce.Member
import model.Eventbrite.EBAccessCode
import org.scalatest.concurrent.ScalaFutures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.Session
import utils.Resource
import model.EventbriteTestObjects._
import scala.concurrent.Future

class DestinationServiceTest extends Specification with Mockito with ScalaFutures {

  "DestinationService" should {

    object DestinationServiceTest extends DestinationService {
      override val contentApiService = mock[GuardianContentService]
      override val memberService = mock[MemberService]
      override val eventbriteService = mock[EventbriteCollectiveServices]
    }

    val destinationService = DestinationServiceTest

    "should return a content destination url if join-referrer is in the request session" in {
      //todo switch to using member request case class with play mock request.
      val request = mock[AnyMemberTierRequest[_]]
      request.session returns mock[Session]
      request.session.get("join-referrer") returns Some("http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts")

      val item = JsonParser.parseItem(Resource.get("model/content.api/item.json"))
      destinationService.contentApiService.contentItemQuery("/membership/2015/apr/17/guardian-live-diversity-in-the-arts") returns Future.successful(item)

      val futureResult = destinationService.contentDestinationFor(request)

      whenReady(futureResult) { contentDestinationOpt =>
        contentDestinationOpt.get.item.content.id mustEqual ("membership/2015/apr/17/guardian-live-diversity-in-the-arts")
      }
    }

    "should return an event destination url if preJoinReturnUrl is in the request session" in {
      val request = mock[AnyMemberTierRequest[_]]
      request.session returns mock[Session]
      val eventId = "0123456"
      request.session.get("preJoinReturnUrl") returns Some("/event/0123456/buy")
      request.member returns mock[Member]

      val event = TestRichEvent(eventWithName().copy(id = eventId))

      destinationService.memberService.createDiscountForMember(request.member, event) returns Future.successful(Some(EBAccessCode("some-discount-code", 2)))
      destinationService.eventbriteService.getBookableEvent(eventId) returns Some(event)

      val futureResult = destinationService.eventDestinationFor(request)

      whenReady(futureResult) { eventDestinationOpt =>
        eventDestinationOpt.get.event.id mustEqual (eventId)
      }
    }
  }
}