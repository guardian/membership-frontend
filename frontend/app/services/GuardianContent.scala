package services

import com.gu.contentapi.client.{GuardianContentApiError, GuardianContentClient}
import com.gu.contentapi.client.model.{ItemQuery, ItemResponse}
import configuration.Config
import monitoring.ContentApiMetrics
import org.joda.time.DateTime
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

trait GuardianContent {

  val contentApi = new GuardianContentClient(Config.contentApiKey)

  def masterclasses(page: Int): Future[ItemResponse] = {
    val date = new DateTime(2014, 1, 1, 0, 0)
    val itemQuery = ItemQuery("guardian-masterclasses")
      .fromDate(date)
      .pageSize(100)
      .page(page)
      .showReferences("eventbrite")
      .showFields("body")
      .showElements("image")
      contentApi.getResponse(itemQuery).andThen {
      case Failure(GuardianContentApiError(status, message)) =>
        ContentApiMetrics.putResponseCode(status, "GET content")
        Logger.error(s"Error response from Content API $status")
    }
  }
}