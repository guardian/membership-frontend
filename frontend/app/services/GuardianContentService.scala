package services

import java.time.Instant

import akka.actor.ActorSystem
import com.gu.contentapi.client.model.{ItemQuery, SearchQuery}
import com.gu.contentapi.client.model.v1._
import com.gu.contentapi.client.{GuardianContentApiError, GuardianContentClient}
import com.gu.memsub.util.ScheduledTask
import configuration.Config
import org.joda.time.DateTime
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import play.api.libs.iteratee.{Enumerator, Iteratee}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

class GuardianContentService(actorSystem: ActorSystem, executionContext: ExecutionContext) extends GuardianContent {

  implicit private val as = actorSystem
  override implicit val ec = executionContext

  private def eventbrite: Future[Seq[Content]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- eventbriteQuery(nextPage)
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
          val masterclassData = response.results.toSeq.flatten.flatMap(MasterclassDataExtractor.extractEventbriteInformation)
          val pagination = ContentAPIPagination(response.currentPage.getOrElse(0), response.pages.getOrElse(0))
          Some(pagination.nextPageOpt, masterclassData)

        }
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  private def offersAndCompetitions: Future[Seq[Content]] = offersAndCompetitionsContentQuery(1).map(_.results.toSeq.flatten)

  private def membershipFront: Future[Seq[Content]] = membershipFrontContentQuery(1).map(_.results.toSeq.flatten)

  def masterclassContent(eventId: String): Option[MasterclassData] = masterclassContentTask.get().find(mc => mc.eventId.equals(eventId))

  def content(eventId: String): Option[Content] = contentTask.get().find(c => c.references.map(_.id).contains(s"eventbrite/$eventId"))

  def offersAndCompetitionsContent: Seq[Content] = offersAndCompetitionsContentTask.get()

  def membershipFrontContent: Seq[Content] = membershipFrontContentTask.get()

  private val contentApiPeriod = 30.minutes

  val masterclassContentTask = ScheduledTask[Seq[MasterclassData]](
    "GuardianContentService - Masterclass content", Nil, 2.seconds, contentApiPeriod)(masterclasses)

  val contentTask = ScheduledTask[Seq[Content]](
    "GuardianContentService - Content with Eventbrite reference", Nil, 1.millis, contentApiPeriod)(eventbrite)

  val offersAndCompetitionsContentTask = ScheduledTask[Seq[Content]](
    "GuardianContentService - Content with Guardian Members Only", Nil, 1.second, contentApiPeriod)(offersAndCompetitions)

  val membershipFrontContentTask = ScheduledTask[Seq[Content]](
    "GuardianContentService - Content from Membership front", Nil, 1.second, contentApiPeriod)(membershipFront)

  def start() {
    masterclassContentTask.start()
    offersAndCompetitionsContentTask.start()
    contentTask.start()
    membershipFrontContentTask.start()
  }

}

trait GuardianContent {

  implicit val ec: ExecutionContext

  val client = new GuardianContentClient(Config.contentApiKey) {
    override val targetUrl = "https://content.guardianapis.com"
  }

  val logAndRecord: PartialFunction[Try[_], Unit] = {
    case Success(_) =>
    case Failure(GuardianContentApiError(status, message, _)) =>
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

  def offersAndCompetitionsContentQuery(page: Int): Future[ItemResponse] = {
    val itemQuery = ItemQuery("membership")
      .showElements("image")
      .showTags("keyword")
      .pageSize(100)
      .page(page)
      .tag("membership/membership-offers|membership/membership-competitions")

    client.getResponse(itemQuery).andThen(logAndRecord)
  }

  def membershipFrontContentQuery(page: Int): Future[ItemResponse] = {
    val itemQuery = ItemQuery("/membership")
      .showElements("image")
      .tag("type/article")
      .pageSize(20)
      .page(page)

    client.getResponse(itemQuery).andThen(logAndRecord)
  }

  def contentItemQuery(path: String): Future[ItemResponse] = {
    val itemQuery = ItemQuery(path)
      .showFields("trailText,membershipAccess")
      .showElements("all")
      .showTags("all")
    client.getResponse(itemQuery).andThen(logAndRecord)
  }
}
