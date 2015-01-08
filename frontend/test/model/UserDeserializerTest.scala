package model

import model.UserDeserializer.readsUser
import org.specs2.mutable.Specification
import play.api.libs.json.JsSuccess
import utils.Resource

class UserDeserializerTest extends Specification {
  "parsing ID API user response" should {
    "handle a user missing their status fields" in {
      val json = Resource.getJson("model/identity/user.without-status-fields.json")
      val jsResult = readsUser.reads(json \ "user")
      jsResult must beAnInstanceOf[JsSuccess[IdUser]]
    }
  }
}
