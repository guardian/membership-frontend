package services

import actions.AnyMemberTierRequest
import com.gu.contentapi.client.parser.JsonParser
import org.scalatest.concurrent.ScalaFutures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.mvc.Session
import utils.Resource

import scala.concurrent.Future


class DestinationServiceTest extends Specification with Mockito with ScalaFutures {


  "DestinationService" should {


    object DestinationServiceTest extends DestinationService {
      override val contentApiService = mock[GuardianContentService]
    }

    val destinationService = DestinationServiceTest

    "should return a content destination url if join-referrer is in the request session" in {
      //todo switch to using member request case class with play mock request.
      val request = mock[AnyMemberTierRequest[_]]
      val session = mock[Session]
      request.session returns session
      request.session.get("join-referrer") returns Some("http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts")
      val item2 = Resource.get("model/content.api/item.json")
      val x = JsonParser.parseItem(item2)

      destinationService.contentApiService.contentItemQuery("/membership/2015/apr/17/guardian-live-diversity-in-the-arts") returns Future.successful(x)

      val futureResult = destinationService.contentDestinationFor(request)

      whenReady(futureResult) { contentDestinationOpt =>
        contentDestinationOpt.get.item.content.id mustEqual ("membership/2015/apr/17/guardian-live-diversity-in-the-arts")
      }
    }
  }
}

