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

class GridService extends utils.WebServiceHelper[GridObject, Error] {

  def isUrlCorrectFormat(url: String) = url.startsWith(Config.gridConfig.url)

  def getEndpoint(url: String): Option[String] =
    if(isUrlCorrectFormat(url)) Some(url.replace(Config.gridConfig.url, ""))
    else None


  //TODO might not need this - after speaking to Seb we just want to get the crop that is supplied.
  def getCropRequested(urlString: String) = {
    val uri = parse(urlString)
    uri.query.param("crop")
  }

  def getAllCrops(endpoint: String) = {
    for { media <- get[GridResult](endpoint)
    } yield media
  }


  override val wsUrl: String = Config.gridConfig.apiUrl

  override def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withHeaders("X-Gu-Media-Key" -> Config.gridConfig.key)

  override val wsMetrics: StatusMetrics = GridApiMetrics
}
