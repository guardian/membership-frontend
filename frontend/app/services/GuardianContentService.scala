package services

import com.gu.contentapi.client.{GuardianContentApiError, GuardianContentClient}
import com.gu.contentapi.client.model._
import configuration.Config
import monitoring.ContentApiMetrics
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.iteratee.{Iteratee, Enumerator}
import utils.ScheduledTask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure
import scala.concurrent.duration._

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

trait GuardianContentService extends GuardianContent {

  private def eventbrite: Future[Seq[Content]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- eventbriteContent(nextPage)
        } yield {
          val pagination = ContentAPIPagination(response.currentPage, response.pages)
          Some(pagination.nextPageOpt, response.results)

        }
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  private def masterclasses: Future[Seq[MasterclassData]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- masterclassesQuery(nextPage)
        } yield {
          val masterclassData = response.results.flatMap(MasterclassDataExtractor.extractEventbriteInformation)
          val pagination = ContentAPIPagination(response.currentPage.getOrElse(0), response.pages.getOrElse(0))
          Some(pagination.nextPageOpt, masterclassData)

        }
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  def masterclassContent(eventId: String): Option[MasterclassData] = masterclassContentTask.get().find(mc => mc.eventId.equals(eventId))
  
  def eventbriteContent(eventId: String): Option[Content] = eventbriteContentTask.get().find(_.references.contains(eventId))

  val masterclassContentTask = ScheduledTask[Seq[MasterclassData]]("GuardianContentService - Masterclass content", Nil, 2.seconds, 2.minutes)(masterclasses)

  val eventbriteContentTask = ScheduledTask[Seq[Content]]("GuardianContentService - Eventbrite content", Nil, 2.seconds, 5.minutes)(eventbrite)

}

object GuardianContentService extends GuardianContentService

trait GuardianContent {

  val client = new GuardianContentClient(Config.contentApiKey)

  def masterclassesQuery(page: Int): Future[ItemResponse] = {
    val date = new DateTime(2014, 1, 1, 0, 0)
    val itemQuery = ItemQuery("guardian-masterclasses")
      .fromDate(date)
      .pageSize(100)
      .page(page)
      .showReferences("eventbrite")
      .showFields("body")
      .showElements("image")
    client.getResponse(itemQuery).andThen {
      case Failure(GuardianContentApiError(status, message)) =>
        logAndRecordError(status)
    }
  }

  def eventbriteContent(page: Int): Future[SearchResponse] = {
    val searchQuery = SearchQuery()
      .referenceType("eventbrite")
      .pageSize(100)
      .page(page)
    client.getResponse(searchQuery).andThen {
      case Failure(GuardianContentApiError(status, message)) =>
        logAndRecordError(status)
    }
  }

  def logAndRecordError(status: Int) {
    ContentApiMetrics.putResponseCode(status, "GET content")
    Logger.error(s"Error response from Content API $status")
  }
}