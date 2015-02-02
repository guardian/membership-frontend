package services

import com.gu.membership.salesforce.Tier
import com.gu.membership.zuora.{Address, Countries}
import controllers.IdentityRequest
import forms.MemberForm._
import model.IdMinimalUser
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{JsObject, Json}
import utils.Resource

class IdentityServiceTest extends Specification with Mockito {

  val user = new IdMinimalUser("4444", Some("Joe Bloggs"))
  val headers = List("headers" -> "a header")
  val trackingParameters = List("some tracking parameters" -> "a tracking param")
  val identityRequest = new IdentityRequest(headers, trackingParameters)

  "IdentityService" should {
    "post json for updating an users email" in {
      val identityAPI = mock[IdentityApi]
      val identityService = new IdentityService(identityAPI)

      identityService.updateEmail(user, "joe.bloggs@awesome-email.com", identityRequest)

      val expectedJson = Json.parse("{\"primaryEmailAddress\": \"joe.bloggs@awesome-email.com\"}").as[JsObject]
      there was one(identityAPI).post("user/4444", expectedJson , headers, trackingParameters, "update-user")
    }

    "post json for updating users details on joining friend" in {
      val identityAPI = mock[IdentityApi]

      val identityService = new IdentityService(identityAPI)

      val friendForm = FriendJoinForm(
        NameForm("Joe", "Bloggs"),
        Address("line one", "line 2", "town", "country", "postcode", Countries.UK),
        MarketingChoicesForm(Some(false), Some(false)),
        None
      )

      identityService.updateUserFieldsBasedOnJoining(user, friendForm, identityRequest)

      val expectedJson = Resource.getJson(s"model/identity/update-friend.json").as[JsObject]
      there was one(identityAPI).post("user/4444", expectedJson, headers, trackingParameters, "update-user")
    }

    "post json for updating users details on joining paid tier" in {
      val identityAPI = mock[IdentityApi]

      val identityService = new IdentityService(identityAPI)

      val paidForm = PaidMemberJoinForm(
        Tier.Partner,
        NameForm("Joe", "Bloggs"),
        PaymentForm(true, "token"),
        Address("line one", "line 2", "town", "country", "postcode", Countries.UK),
        Some(Address("line one", "line 2", "town", "country", "postcode", Countries.UK)),
        MarketingChoicesForm(Some(false), Some(false)),
        None
      )

      identityService.updateUserFieldsBasedOnJoining(user, paidForm, identityRequest)

      val expectedJson = Resource.getJson(s"model/identity/update-paid.json").as[JsObject]
      there was one(identityAPI).post("user/4444", expectedJson, headers, trackingParameters, "update-user")
    }
  }

  "post json for updating details on upgrade" in {
    val identityAPI = mock[IdentityApi]

    val identityService = new IdentityService(identityAPI)
    val paidMemberChangeForm = FreeMemberChangeForm(
      PaymentForm(true, "token"),
      Address("line one", "line 2", "town", "country", "postcode", Countries.UK),
      Some(Address("line one", "line 2", "town", "country", "postcode", Countries.UK))
    )

    identityService.updateUserFieldsBasedOnUpgrade(user, paidMemberChangeForm, identityRequest)

    val expectedJson = Resource.getJson(s"model/identity/update-upgrade.json").as[JsObject]
    there was one(identityAPI).post("user/4444", expectedJson, headers, trackingParameters, "update-user")

  }
}
