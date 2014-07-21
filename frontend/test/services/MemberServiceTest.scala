package services

import scala.concurrent.Future

import play.api.libs.ws.WSResponse
import play.api.libs.json.{Json, JsValue}

import model.{Tier, Member}
import com.gu.scalaforce.Scalaforce
import org.specs2.mutable.Specification

class MemberServiceTest extends Specification {

  case class RequestInfo(method: String, url: String, body: Any = null)

  "MemberServiceTest" should {

    "update a member" in TestMemberService { service =>
      service.update(Member("userId", Tier.Partner, "customerId"))

      service.lastRequest mustEqual RequestInfo(
        "PATCH",
        "/services/data/v29.0/sobjects/Contact/IdentityID__c/userId",
        Json.obj(
          "Stripe_Customer_ID__c" -> "customerId",
          "LastName" -> "LAST NAME",
          "Membership_Tier__c" -> "Partner"
        )
      )
    }

    "get a member by id" in TestMemberService { service =>
      service.get("userId")
      service.lastRequest mustEqual RequestInfo(
        "GET",
        "/services/data/v29.0/sobjects/Contact/IdentityID__c/userId"
      )
    }

    "get a member by customer id" in TestMemberService { service =>
      service.getByCustomerId("customerId")
      service.lastRequest mustEqual RequestInfo(
        "GET",
        "/services/data/v29.0/sobjects/Contact/Stripe_Customer_ID__c/customerId"
      )
    }

    "generate a session" in TestMemberService { service =>
      service.salesforce.getAccessToken
      service.lastRequest mustEqual RequestInfo(
        "POST",
        "/services/oauth2/token",
        Seq(
          "client_id" -> "consumerKey",
          "client_secret" -> "consumerSecret",
          "username" -> "testUser",
          "password" -> "testPasstestToken",
          "grant_type" -> "password"
        )
      )
    }
  }

  class TestMemberService extends MemberService {
    var lastRequest: RequestInfo = _

    val salesforce = new Scalaforce {
      val consumerKey: String = "consumerKey"
      val consumerSecret: String = "consumerSecret"

      val apiURL: String = "http://www.test.com"
      val apiUsername: String = "testUser"
      val apiPassword: String = "testPass"
      val apiToken: String = "testToken"

      def get(endpoint: String): Future[WSResponse] = {
        lastRequest = RequestInfo("GET", endpoint)
        Future.failed(new Exception("Not implemented"))
      }

      def patch(endpoint: String, body: JsValue): Future[WSResponse] = {
        lastRequest = RequestInfo("PATCH", endpoint, body)
        Future.failed(new Exception("Not implemented"))
      }

      def login(endpoint: String, params: Seq[(String, String)]): Future[WSResponse] = {
        lastRequest = RequestInfo("POST", endpoint, params)
        Future.failed(new Exception("Not implemented"))
      }
    }
  }

  object TestMemberService {
    def apply[T](block: TestMemberService => T) = block(new TestMemberService)
  }
}
