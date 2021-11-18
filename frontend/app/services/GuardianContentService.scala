package services

import akka.actor.ActorSystem
import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.model.{ContentApiError, ItemQuery, SearchQuery}
import com.gu.memsub.util.ScheduledTask
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import configuration.Config

import java.time.Instant
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

class GuardianContentService(actorSystem: ActorSystem, executionContext: ExecutionContext) extends GuardianContent {

  implicit private val as = actorSystem
  override implicit val ec = executionContext

  private def eventbrite(nextPage: Int = 1, accumulatedEventbriteData: Seq[Content] = Nil): Future[Seq[Content]] = {
    for {
      response <- eventbriteQuery(nextPage)
      rest <- {
        val pagination = ContentAPIPagination(response.currentPage, response.pages)
        val combined = response.results.toSeq.reverse ++ accumulatedEventbriteData // linked list - prepend the smaller amount
        pagination.nextPageOpt match {
          case Some(nextPage) => eventbrite(nextPage, combined)
          case None => Future.successful(combined.reverse) // get back the original order
        }
      }
    } yield rest
  }

  private def masterclasses(nextPage: Int = 1, accumultedMaasterclassData: Seq[MasterclassData] = Nil): Future[Seq[MasterclassData]] = {
    for {
      response <- masterclassesQuery(nextPage)
      rest <- {
        val masterclassData = response.results.toSeq.flatten.flatMap(MasterclassDataExtractor.extractEventbriteInformation)
        val pagination = ContentAPIPagination(response.currentPage.getOrElse(0), response.pages.getOrElse(0))
        val combined = masterclassData.reverse ++ accumultedMaasterclassData // linked list - prepend the smaller amount
        pagination.nextPageOpt match {
          case Some(nextPage) => masterclasses(nextPage, combined)
          case None => Future.successful(combined.reverse) // get back the original order
        }
      }
    } yield rest
  }

  def masterclassContent(eventId: String): Option[MasterclassData] = masterclassContentTask.get().find(mc => mc.eventId.equals(eventId))

  def content(eventId: String): Option[Content] = contentTask.get().find(c => c.references.map(_.id).contains(s"eventbrite/$eventId"))

  private val contentApiPeriod = 30.minutes

  val masterclassContentTask = ScheduledTask[Seq[MasterclassData]](
    "GuardianContentService - Masterclass content", Nil, 2.seconds, contentApiPeriod)(masterclasses())

  val contentTask = ScheduledTask[Seq[Content]](
    "GuardianContentService - Content with Eventbrite reference", Nil, 1.millis, contentApiPeriod)(eventbrite())


  def start() = {
    masterclassContentTask.start()
    contentTask.start()
  }

}

trait GuardianContent {

  implicit val ec: ExecutionContext

  val client = new GuardianContentClient(Config.contentApiKey) {
    override val targetUrl = "https://content.guardianapis.com"
  }

  val logAndRecord: PartialFunction[Try[_], Unit] = {
    case Success(_) =>
    case Failure(ContentApiError(status, message, _)) =>
      SafeLogger.error(scrub"Error response from Content API $status")
  }

  def masterclassesQuery(page: Int): Future[ItemResponse] = {
    val date = Instant.parse("2014-01-01T00:00:00Z")

    val itemQuery = ItemQuery("guardian-masterclasses")
      .fromDate(Some(date))
      .pageSize(100)
      .page(page)
      .showReferences("eventbrite")
      .showFields("body")
      .showElements("image")

    client.getResponse(itemQuery).andThen(logAndRecord)
  }

  def eventbriteQuery(page: Int): Future[SearchResponse] = {
    val searchQuery = SearchQuery()
      .referenceType("eventbrite")
      .showReferences("eventbrite")
      .showElements("image")
      .pageSize(100)
      .page(page)
    client.getResponse(searchQuery).andThen(logAndRecord)
  }
}
