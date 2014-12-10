package services

import controllers.IdentityRequest
import model.IdMinimalUser
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class IdentityServiceTest extends Specification with Mockito{

  val identityAPI = mock[IdentityApi]
  val identityService = new IdentityService(identityAPI)

  val user = new IdMinimalUser("4444", Some("Joe Bloggs"))
  val headers = List("headers" -> "a header")
  val trackingParameters = List("some tracking parameters" -> "a tracking param")
  val identityRequest = new IdentityRequest(headers, trackingParameters)

  "IdentityService" should {
    "post json for updating an users email" in {

      identityService.updateEmail(user, "joe.bloggs@awesome-emaill.com", identityRequest)
      there was one(identityAPI).post("user/4444", Json.obj("primaryEmailAddress" -> "joe.bloggs@awesome-emaill.com"), headers, trackingParameters, "update-user")
    }
  }

}