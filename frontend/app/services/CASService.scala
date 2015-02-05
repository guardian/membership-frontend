package services

import scala.concurrent.{ExecutionContext, Future}

import play.api.libs.json.Json
import play.api.libs.ws.WSRequestHolder

import com.gu.membership.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics

import configuration.Config
import model.CAS._
import model.CAS.Deserializer._
import monitoring.Metrics

object CASService extends WebServiceHelper[CASResult, CASError] {
  override val wsUrl = Config.casURL

  override def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req

  override val wsMetrics = new Metrics with StatusMetrics {
    override val service = "CAS"
  }

  def check(subscriberId: String, password: String)(implicit ec: ExecutionContext): Future[CASResult] =
    post[CASSuccess]("subs", Json.obj(
      "appId" -> "membership.theguardian.com",
      "deviceId" -> "",
      "subscriberId" -> subscriberId,
      "password" -> password
    )).recover {
      case error: CASError => error
    }
}
