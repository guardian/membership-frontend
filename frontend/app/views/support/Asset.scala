package views.support

import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable.{Map => MutableMap}
import scala.io.Source

object Asset {
  lazy val map = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse(_))
    jsonOpt.map(_.as[JsObject].fields.toMap.mapValues(_.as[String])).getOrElse(Map.empty)
  }

  def at(path: String): String = "/assets/" + map.getOrElse(path, path)
}
