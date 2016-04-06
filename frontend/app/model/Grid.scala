package model

import play.api.libs.json.Json

object Grid {
  trait GridObject

  case class Error(message: String) extends Throwable with GridObject

  case class GridResult(data: Data) extends GridObject

  case class Data(
      id: String, metadata: Metadata, exports: Option[List[Export]])

  case class Metadata(description: Option[String],
                      credit: Option[String],
                      byline: Option[String]) {
    val photographer = (byline ++ credit).mkString("/")
  }

  case class Export(id: String, assets: List[Asset], master: Option[Asset])

  case class Asset(
      file: String, secureUrl: Option[String], dimensions: Dimensions) {
    lazy val pixels = dimensions.width * dimensions.height
  }

  case class Dimensions(height: Int, width: Int)
}

object GridDeserializer {
  import Grid._

  implicit val readsDimensions = Json.reads[Dimensions]
  implicit val readsAsset = Json.reads[Asset]
  implicit val readsExport = Json.reads[Export]
  implicit val readsMetadata = Json.reads[Metadata]
  implicit val readsData = Json.reads[Data]
  implicit val readsGrid = Json.reads[GridResult]
  implicit val readsError = Json.reads[Error]
}
