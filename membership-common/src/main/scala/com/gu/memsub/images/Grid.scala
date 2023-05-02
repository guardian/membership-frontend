package com.gu.memsub.images

import play.api.libs.json.Json

object Grid {
  trait GridObject

  case class Error(message: String) extends Throwable with GridObject

  case class GridResult(data: Data) extends GridObject

  case class Data(id: String, metadata: Metadata, exports: Option[List[Export]])

  case class Metadata(description: Option[String], credit: Option[String], byline: Option[String]) {
    val photographer = (byline ++ credit).mkString("/")
  }

  case class Export(id: String, assets: List[Asset], master: Option[Asset])

  case class Asset(file: String, secureUrl: Option[String], dimensions: Dimensions) {
    lazy val pixels = dimensions.width * dimensions.height
  }

  case class Dimensions(height: Int, width: Int)

}
object GridDeserializer {
  import Grid._

  implicit val readsDimensions = Json.format[Dimensions]
  implicit val readsAsset = Json.format[Asset]
  implicit val readsExport = Json.format[Export]
  implicit val readsMetadata = Json.format[Metadata]
  implicit val readsData = Json.format[Data]
  implicit val readsGrid = Json.format[GridResult]
  implicit val readsError = Json.format[Error]
}

