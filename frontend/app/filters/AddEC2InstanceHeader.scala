package filters

import javax.inject.Inject

import akka.stream.Materializer
import configuration.Config
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class AddEC2InstanceHeader(wsClient: WSClient)(implicit val mat: Materializer, implicit val ec: ExecutionContext) extends Filter {

  // http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
  lazy val instanceIdOptF = if (Config.stageProd)
    wsClient.url("http://169.254.169.254/latest/meta-data/instance-id").get().map(resp => Some(resp.body).filter(_.nonEmpty)).recover { case _ => None }
  else
    Future(None)

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
    instanceIdOpt <- instanceIdOptF
  } yield instanceIdOpt.fold(result)(instanceId => result.withHeaders("X-EC2-instance-id" -> instanceId))

}
