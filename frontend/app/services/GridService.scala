package services

import com.gu.monitoring.StatusMetrics
import com.netaporter.uri.Uri.parse
import configuration.Config
import model.Eventbrite.EventImage
import model.Grid
import model.Grid._
import model.GridDeserializer._
import monitoring.GridApiMetrics
import play.api.libs.ws.WSRequestHolder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GridConfig(url: String, apiUrl: String, key: String)

case class GridService(gridUrl: String) extends utils.WebServiceHelper[GridObject, Error] {

  def isUrlCorrectFormat(url: String) = url.startsWith(gridUrl)

  def getEndpoint(url: String) = url.replace(gridUrl, "")

  def cropParam(urlString: String) = parse(urlString).query.param("crop")

  def getRequestedCrop(url: String) : Future[Option[EventImage]] = {
    for (gridOpt <- getGrid(url))
    yield {
      gridOpt.flatMap { grid =>
        grid.data.exports.map(exports => EventImage(findAssets(exports, cropParam(url)), grid.data.metadata))
      }
    }
  }

  def getGrid(url: String) = {
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
