package filters

import com.gu.lib.okhttpscala._
import com.squareup.okhttp
import com.squareup.okhttp.OkHttpClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

object AddEC2InstanceHeader extends Filter {

  // http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
  lazy val instanceIdOptF =
    new OkHttpClient().execute(new okhttp.Request.Builder().url("http://169.254.169.254/latest/meta-data/instance-id").build()).map(resp => Some(resp.body.string).filter(_.nonEmpty)).recover { case _ => None }

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
    instanceIdOpt <- instanceIdOptF
  } yield instanceIdOpt.fold(result)(instanceId => result.withHeaders("X-EC2-instance-id" -> instanceId))

}
