package services

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator

import akka.agent.Agent
import com.gu.memsub.images.Grid
import com.gu.memsub.images.Grid.{Export, GridObject, GridResult}
import com.gu.memsub.images.GridDeserializer._
import com.gu.memsub.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics
import com.gu.okhttp.RequestRunners
import com.gu.okhttp.RequestRunners.LoggingHttpClient
import com.netaporter.uri.Uri
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import model.RichEvent.GridImage
import monitoring.GridApiMetrics
import okhttp3.Request
import play.api.libs.json.Json

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

object GridService extends WebServiceHelper[GridObject, Grid.Error] with LazyLogging {


  val gridUrl: String = "https://media.gutools.co.uk/images/"
  val CropQueryParam = "crop"

  case class ImageIdWithCrop(id: String, crop: String)
  object ImageIdWithCrop {
    implicit val writesImageIdWithCrop = Json.writes[ImageIdWithCrop]

    def fromGuToolsUri(uri: Uri): Option[ImageIdWithCrop] =
      for {
        imageId <- uri.path.split("/").lastOption
        crop <-  uri.query.param(CropQueryParam)
        if uri.toString().startsWith(gridUrl)
      } yield ImageIdWithCrop(imageId, crop)
  }

  private lazy val atomicReference = new AtomicReference[Map[ImageIdWithCrop, GridImage]](Map.empty)

  // todo: remove once we upgrade to scala 2.12
  implicit def unarayConverter[T](f: T => T) = new UnaryOperator[T] {
    override def apply(t: T): T = f(t)
  }

  def getRequestedCrop(gridId: ImageIdWithCrop) : Future[Option[GridImage]] = {
    val currentImageData = atomicReference.get()
    if(currentImageData.contains(gridId)) Future.successful(currentImageData.get(gridId))
    else {
      getGrid(gridId).map { grid =>
        for {
          exports <- grid.data.exports
          export <- findExport(exports, gridId.crop)
          if export.assets.nonEmpty
        } yield {
          val image = GridImage(export.assets, grid.data.metadata, export.master)
          atomicReference.updateAndGet({ oldImageData: Map[ImageIdWithCrop, GridImage] =>
            val newImageData = oldImageData + (gridId -> image)
            logger.trace(s"Adding image $gridId to the event image map")
            newImageData
          })
          image
        }
      }
    }
  }.recover { case e =>
    logger.error(s"Error getting crop for $gridId", e)
    None
  } // We should return no image, rather than die

  private [services] def getGrid(gridId: ImageIdWithCrop): Future[GridResult] =
    get[Grid.GridResult](gridId.id, CropQueryParam -> gridId.crop)

  private [services] def findExport(exports: List[Export], cropId: String): Option[Export] = exports.find(_.id == cropId)

  override val wsUrl: String = Config.gridConfig.apiUrl

  override def wsPreExecute(req: Request.Builder): Request.Builder = req.addHeader("X-Gu-Media-Key", Config.gridConfig.key)

  override val httpClient: LoggingHttpClient[Future] = RequestRunners.loggingRunner(GridApiMetrics)
}

case class GridConfig(apiUrl: String, key: String)

