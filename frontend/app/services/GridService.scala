package services

import akka.agent.Agent
import com.gu.membership.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics
import com.netaporter.uri.Uri
import com.squareup.okhttp.Request
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.Grid._
import model.GridDeserializer._
import model.RichEvent.GridImage
import monitoring.GridApiMetrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GridService {
  val CropQueryParam = "crop"
}
case class GridService(gridUrl: String) extends WebServiceHelper[GridObject, Error] with LazyLogging{
  import GridService._

  lazy val agent = Agent[Map[Uri, GridImage]](Map.empty)

  case class ImageIdWithCrop(id: String, crop: String)
  object ImageIdWithCrop {
    def fromGuToolsUri(uri: Uri): Option[ImageIdWithCrop] =
      for {
        imageId <- uri.path.split("/").lastOption
        crop <-  uri.query.param(CropQueryParam)
        if uri.toString().startsWith(gridUrl)
      } yield ImageIdWithCrop(imageId, crop)
  }

  def cropParam(url: Uri) = url.query.param(CropQueryParam)

  def getRequestedCrop(url: Uri) : Future[Option[GridImage]] = {
    val currentImageData = agent.get()
    if(currentImageData.contains(url)) Future.successful(currentImageData.get(url))
    else {
      getGrid(url).map { gridOpt =>
        for {
          grid <- gridOpt
          exports <- grid.data.exports
          assets = findAssets(exports, cropParam(url))
          if assets.nonEmpty
        } yield {
          val image = GridImage(assets, grid.data.metadata)
          agent send {
            oldImageData =>
              val newImageData = oldImageData + (url -> image)
              logger.trace(s"Adding image $url to the event image map")
              newImageData
          }
          image
        }
      }
    }
  }.recover { case e =>
    logger.error(s"Error getting crop for $url", e)
    None
  } // We should return no image, rather than die

  def getGrid(url: Uri): Future[Option[GridResult]] =
    ImageIdWithCrop.fromGuToolsUri(url).map { case ImageIdWithCrop(id, cropId) =>
      get[GridResult](id, CropQueryParam -> cropId).map(Some(_))
    } getOrElse Future.successful(None)

  def findAssets(exports: List[Export], cropParameter: Option[String]) = {
    val requestedExport = cropParameter.flatMap(cr => exports.find(_.id == cr)).orElse(exports.headOption)
    requestedExport.map(_.assets).getOrElse(Nil)
  }

  override val wsUrl: String = Config.gridConfig.apiUrl

  override def wsPreExecute(req: Request.Builder): Request.Builder = req.addHeader("X-Gu-Media-Key", Config.gridConfig.key)

  override val wsMetrics: StatusMetrics = GridApiMetrics
}

case class GridConfig(url: String, apiUrl: String, key: String)

