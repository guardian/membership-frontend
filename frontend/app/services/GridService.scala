package services

import com.gu.monitoring.StatusMetrics
import com.netaporter.uri.Uri.parse
import configuration.Config
import model.Grid.{Error, GridObject, GridResult}
import model.GridDeserializer._
import monitoring.GridApiMetrics
import play.api.libs.ws.WSRequestHolder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class GridConfig(url: String, apiUrl: String, key: String)

object GridService extends utils.WebServiceHelper[GridObject, Error] {

  def isUrlCorrectFormat(url: String) = url.startsWith(Config.gridConfig.url)

  def getEndpoint(url: String) = url.replace(Config.gridConfig.url, "")

  def getCropRequested(urlString: String) = {
    val uri = parse(urlString)
    uri.query.param("crop")
  }

  def getAllCrops(url: String) = {
    if(isUrlCorrectFormat(url)) get[GridResult](getEndpoint(url)).map(Some(_))
    else Future.successful(None)
  }

  override val wsUrl: String = Config.gridConfig.apiUrl

  override def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withHeaders("X-Gu-Media-Key" -> Config.gridConfig.key)

  override val wsMetrics: StatusMetrics = GridApiMetrics
}
