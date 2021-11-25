package views.support


import com.amazonaws.util.IOUtils
import play.api.libs.json.{JsObject, Json}
import play.twirl.api.Html

import scala.io.Source
import configuration.Config

import scala.util.Try

object Asset {
  lazy val map = {
    val resourceOpt = Option(getClass.getClassLoader.getResourceAsStream("assets.map"))
    val jsonOpt = resourceOpt.map(Source.fromInputStream(_).mkString).map(Json.parse(_))
    jsonOpt.map(_.as[JsObject].fields.toMap.view.mapValues(_.as[String]).toMap).getOrElse(Map.empty)
  }

  def at(path: String): String = "/assets/" + map.getOrElse(path, path)
  def pathAt(path: String): String = "public/" + map.getOrElse(path, path)

  // These are excluded from asset hashing because we can't update the URL once someone has saved their bookmarklet.
  // We also want to have the same URL whether assets are compiled for prod or dev.
  def bookmarklet(path: String): String = Config.membershipUrl + "/assets/bookmarklets/" + path

  def inlineResource(path: String): Option[String] = {
    val resource = Try { getClass.getClassLoader.getResourceAsStream(pathAt(path)) }.toOption
    resource.map(file => IOUtils.toString(file))
  }

  def inlineSvg(name: String): Option[Html] = inlineResource("images/inline-svgs/" + name + ".svg").map(Html(_))
}
