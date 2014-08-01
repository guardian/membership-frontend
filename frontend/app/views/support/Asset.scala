package views.support

import scala.io.Source

import play.api.libs.json.{Json, JsObject}
import collection.mutable.{ Map => MutableMap }

object Asset {
  lazy val map = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse(_))
    jsonOpt.map(_.as[JsObject].fields.toMap.mapValues(_.as[String])).getOrElse(Map.empty)
  }

  def at(path: String): String = "/assets/" + map.getOrElse(path, path)
}