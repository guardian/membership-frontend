package filters

import com.amazonaws.internal.EC2MetadataClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

object AddEC2InstanceHeader extends Filter {

  val instanceIdF = Future { new EC2MetadataClient().readResource("instance-id") }.recover { case _ => "n/a" }

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
    instanceId <- instanceIdF
  } yield result.withHeaders("X-EC2-instance-id" -> instanceId)
}
