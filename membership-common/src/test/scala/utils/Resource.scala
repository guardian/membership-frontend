package utils

import play.api.libs.json.JsValue
import play.api.libs.json.Json.parse

import scala.io.Source
import scala.xml.{Elem, XML}

object Resource {
  def get(name: String): String =
    Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name)).mkString

  def getJson(name: String): JsValue = parse(get(name))

  def getXML(name: String): Elem = XML.loadString(get(name))
}
