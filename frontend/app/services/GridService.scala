package services

import com.gu.monitoring.StatusMetrics
import com.netaporter.uri.Uri.parse
import configuration.Config
import model.Grid.{Error, GridObject, GridResult}
import model.GridDeserializer._
import monitoring.GridApiMetrics
import play.api.libs.ws.WSRequestHolder

import scala.concurrent.ExecutionContext.Implicits.global

case class GridConfig(url: String, apiUrl: String, key: String)

object GridService extends utils.WebServiceHelper[GridObject, Error] {

  //todo remove?
  def isUrlCorrectFormat(url: String) = url.startsWith(Config.gridConfig.url)

  def getEndpoint(url: String) = url.replace(Config.gridConfig.url, "")


  //TODO might not need this - after speaking to Seb we just want to get the crop that is supplied.
  def getCropRequested(urlString: String) = {
    val uri = parse(urlString)
    uri.query.param("crop")
  }

  def getAllCrops(url: String) = {
    for { media <- get[GridResult](getEndpoint(url))
    } yield media
  }


  override val wsUrl: String = Config.gridConfig.apiUrl

  override def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withHeaders("X-Gu-Media-Key" -> Config.gridConfig.key)

  override val wsMetrics: StatusMetrics = GridApiMetrics
}
