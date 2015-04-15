package views.support


import com.amazonaws.util.IOUtils
import play.api.Play
import play.api.Play.current
import play.api.libs.json.{JsObject, Json}
import play.twirl.api.Html
import scala.io.Source

object Asset {
  lazy val map = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse(_))
    jsonOpt.map(_.as[JsObject].fields.toMap.mapValues(_.as[String])).getOrElse(Map.empty)
  }

  def at(path: String): String = "/assets/" + map.getOrElse(path, path)
  def pathAt(path: String): String = "public/" + map.getOrElse(path, path)

  def inlineResource(path: String): Option[String] = {
    val resource = Play.resourceAsStream(pathAt(path))
    resource.map(file => IOUtils.toString(file))
  }

  def inlineSvg(name: String): Option[Html] = inlineResource("images/inline-svgs/" + name + ".svg").map(Html(_))
}
