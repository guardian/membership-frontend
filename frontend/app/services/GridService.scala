package services

import akka.agent.Agent
import com.gu.membership.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics
import com.netaporter.uri.Uri
import com.netaporter.uri.Uri.parse
import com.typesafe.scalalogging.slf4j.LazyLogging
import configuration.Config
import model.Grid._
import model.GridDeserializer._
import model.RichEvent.EventImage
import monitoring.GridApiMetrics
import play.api.libs.ws.WSRequestHolder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GridService(gridUrl: String) extends WebServiceHelper[GridObject, Error] with LazyLogging{

  lazy val agent = Agent[Map[Uri, EventImage]](Map.empty)

  def isUrlCorrectFormat(url: Uri) = url.toString.startsWith(gridUrl)

  def getEndpoint(url: Uri) = url.toString.replace(gridUrl, "")

  def cropParam(url: Uri) = url.query.param("crop")

  def getRequestedCrop(url: Uri) : Future[Option[EventImage]] = {
    val currentImageData = agent.get()
    if(currentImageData.contains(url)) Future.successful(currentImageData.get(url))
    else {
      getGrid(url).map { gridOpt =>
        for {
          grid <- gridOpt
          exports <- grid.data.exports
        } yield {
          val image = EventImage(findAssets(exports, cropParam(url)), grid.data.metadata)
          agent send {
            oldImageData =>
              val newImageData = oldImageData + (url -> image)
              logger.info(s"Adding image $url to the event image map")
              newImageData
          }
          image
        }
      }
    }
  }

  def getGrid(url: Uri) = {
    if (isUrlCorrectFormat(url)) get[GridResult](getEndpoint(url)).map(Some(_))
    else Future.successful(None)
  }

  def findAssets(exports: List[Export], cropParameter: Option[String]) = {
    val requestedExport = cropParameter.flatMap(cr => exports.find(_.id == cr)).orElse(exports.headOption)
    requestedExport.map(_.assets).getOrElse(Nil)
  }

  override val wsUrl: String = Config.gridConfig.apiUrl

  override def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withHeaders("X-Gu-Media-Key" -> Config.gridConfig.key)

  override val wsMetrics: StatusMetrics = GridApiMetrics
}

case class GridConfig(url: String, apiUrl: String, key: String)

