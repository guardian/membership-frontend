package utils

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{DiscardingCookie, Cookie}

object GuMemCookie {

  val key = "GU_MEM"

  def encodeUserJson(json: JsObject): String = {
    Base64.encodeBase64URLSafeString(Json.stringify(json).getBytes("UTF-8"))
  }

  def getAdditionCookie(json: JsObject): Cookie = {
    Cookie(key, encodeUserJson(json), secure = true, httpOnly = false)
  }

  val deletionCookie : DiscardingCookie = {
    DiscardingCookie(key)
  }

}
