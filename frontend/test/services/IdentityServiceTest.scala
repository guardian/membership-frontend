package services

import com.gu.i18n.Country
import com.gu.identity.play.IdMinimalUser
import com.gu.identity.play.idapi.UpdateIdUser
import com.gu.memsub.Address
import com.gu.memsub.BillingPeriod.Year
import com.gu.salesforce.Tier.partner
import controllers.IdentityRequest
import forms.MemberForm._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.{JsObject, Json}
import services.IdentityService.DisregardResponseContent
import utils.Resource

class IdentityServiceTest extends Specification with Mockito {

  val ip = "8.8.8.8"
  val user = new IdMinimalUser("4444", Some("Joe Bloggs"))
  val headers = List("headers" -> "a header")
  val trackingParameters = List("some tracking parameters" -> "a tracking param")
  implicit val identityRequest = new IdentityRequest(ip, headers, trackingParameters)

  "IdentityService" should {
    "post json for updating an users email" in {
      val identityAPI = mock[IdentityApi]
      val identityService = IdentityService(identityAPI)

      identityService.updateEmail(user, "joe.bloggs@awesome-email.com")(identityRequest)

      val expectedJson = Json.parse("{\"primaryEmailAddress\": \"joe.bloggs@awesome-email.com\"}").as[JsObject]
      there was one(identityAPI).post("user/4444", Some(expectedJson) , headers, trackingParameters, "update-user")(DisregardResponseContent)
    }

    "post json for updating users details on joining friend" in {
      val identityAPI = mock[IdentityApi]

      val identityService = IdentityService(identityAPI)

      val friendForm = FriendJoinForm(
        NameForm("Joe", "Bloggs"),
        Address("line one", "line 2", "town", "country", "postcode", Country.UK.name),
        MarketingChoicesForm(Some(false), Some(false)),
        None
      )

      identityService.updateUser(UpdateIdUser(privateFields = Some(IdentityService.privateFieldsFor(friendForm))), user.id)

      val expectedJson = Resource.getJson(s"model/identity/update-friend.json").as[JsObject]
      there was one(identityAPI).post("user/4444", Some(expectedJson), headers, trackingParameters, "update-user")(DisregardResponseContent)
    }

    "post json for updating users details on joining paid tier" in {
      val identityAPI = mock[IdentityApi]

      val identityService = IdentityService(identityAPI)

      val paidForm = PaidMemberJoinForm(
        partner,
        NameForm("Joe", "Bloggs"),
        "joe@example.com",
        PaymentForm(Year, Some("stripeToken"), None),
        Address("line one", "line 2", "town", "country", "postcode", Country.UK.name),
        Some(Address("line one", "line 2", "town", "country", "postcode", Country.UK.name)),
        MarketingChoicesForm(Some(false), Some(false)),
        None,
        None,
        subscriberOffer = false,
        Set.empty,
        None
      )

      identityService.updateUser(UpdateIdUser(privateFields = Some(IdentityService.privateFieldsFor(paidForm))), user.id)

      val expectedJson = Resource.getJson(s"model/identity/update-paid.json").as[JsObject]
      there was one(identityAPI).post("user/4444", Some(expectedJson), headers, trackingParameters, "update-user")(DisregardResponseContent)
    }
  }

  "post json for updating details on upgrade" in {
    val identityAPI = mock[IdentityApi]

    val identityService = IdentityService(identityAPI)
    val addressDetails = AddressDetails(
      Address("line one", "line 2", "town", "country", "postcode", Country.UK.name),
      Some(Address("line one", "line 2", "town", "country", "postcode", Country.UK.name))
    )

    identityService.updateUserFieldsBasedOnUpgrade(user.id, addressDetails)(identityRequest)

    val expectedJson = Resource.getJson(s"model/identity/update-upgrade.json").as[JsObject]
    there was one(identityAPI).post("user/4444", Some(expectedJson), headers, trackingParameters, "update-user")(DisregardResponseContent)
  }
}
