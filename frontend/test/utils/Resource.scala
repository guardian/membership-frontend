package utils

import scala.io.Source
import play.api.libs.json.{Json, JsValue}
import scala.xml.{XML, Elem}

object Resource {
  def get(name: String): String = {
    val out = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(name)).mkString
    println(out)
    out
  }

  def getJson(name: String): JsValue = Json.parse(get(name))

  def getXML(name: String): Elem = XML.loadString(get(name))
}
