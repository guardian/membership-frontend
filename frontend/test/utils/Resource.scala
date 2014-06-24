package utils

import scala.io.Source
import play.api.libs.json.{Json, JsValue}

object Resource {
  def get(name: String): String =
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name)).mkString

  def getJson(name: String): JsValue = Json.parse(get(name))

}
