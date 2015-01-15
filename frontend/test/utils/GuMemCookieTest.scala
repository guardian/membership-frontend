package utils

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import utils.GuMemCookie.encodeUserJson

class GuMemCookieTest extends Specification {

  val userTestData = Json.obj(
    "userId" -> 123456,
    "regNumber" -> "",
    "firstName" -> "test",
    "tier" -> "Friend",
    "joinDate" -> 1421252372000L
  )

  "encodeUserJson" should {
    "encode json to base64 without padding" in {
      encodeUserJson(userTestData) mustEqual "eyJ1c2VySWQiOjEyMzQ1NiwicmVnTnVtYmVyIjoiIiwiZmlyc3ROYW1lIjoidGVzdCIsInRpZXIiOiJGcmllbmQiLCJqb2luRGF0ZSI6MTQyMTI1MjM3MjAwMH0"
    }
  }

}
