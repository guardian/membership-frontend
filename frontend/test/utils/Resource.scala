package utils

import scala.io.{Codec, Source}
import play.api.libs.json.{JsValue, Json}

import scala.xml.{Elem, XML}

object Resource {
  def get(name: String): String =
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name))(Codec.UTF8).mkString

  def getJson(name: String): JsValue = Json.parse(get(name))

  def getXML(name: String): Elem = XML.loadString(get(name))
}
