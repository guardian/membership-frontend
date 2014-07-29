package views.support

import scala.io.Source

import play.api.libs.json.{Json, JsObject}
import collection.mutable.{ Map => MutableMap }

object Asset {
  lazy val map = {
    val json = Json.parse(Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("assets.map")).mkString)
    json.as[JsObject].fields.toMap.mapValues(_.as[String])
  }

  def at(path: String): String = "/assets/dist/" + map.getOrElse(path, path)
}