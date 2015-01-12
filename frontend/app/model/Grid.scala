package model

import play.api.libs.json.Json

object Grid {
  trait GridObject

  case class Error(message: String) extends Throwable with GridObject

  case class GridResult(uri: String, data: Data) extends GridObject

  case class Data(id: String, metadata: Metadata, exports: List[Export])

  case class Metadata(description: Option[String], credit: Option[String], byline: Option[String]) {

    val photographer = s"${byline.getOrElse("")}${credit.map(c => s"/$c").getOrElse("")}"
  }

  case class Export(id: String, assets: List[Asset])

  case class Asset(file: String, secureFile: Option[String], dimensions: Dimensions)

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