package views.support

import scala.io.Source

import play.api.libs.json.{Json, JsObject}
import collection.mutable.{ Map => MutableMap }
import configuration.Config

object Asset {
  lazy val map = {
    val json = Json.parse(Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("assets.map")).mkString)
    json.as[JsObject].fields.toMap.mapValues(_.as[String])
  }

  def at(path: String):
    String = "/assets/" + (if (Config.membershipDebug) path else "dist/" + map.getOrElse(path, path))
}