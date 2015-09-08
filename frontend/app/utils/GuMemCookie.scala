package utils

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsObject, Json}

object GuMemCookie {

  def encodeUserJson(json: JsObject): String = {
    Base64.encodeBase64URLSafeString(Json.stringify(json).getBytes("UTF-8"))
  }

}
