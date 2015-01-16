package services

import akka.agent.Agent
import com.gu.contentapi.client.model.{Asset, Content, ItemResponse}
import com.gu.contentapi.client.{GuardianContentApiError, GuardianContentClient}
import configuration.Config
import monitoring.ContentApiMetrics
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.matching.Regex

case class MasterclassData(eventId: String, webUrl: String, images: List[Asset])

case class MasterclassResponse[T](pagination: ContentAPIPagination, data: Seq[T])

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

trait MasterclassDataService {

  val contentApi = new GuardianContentClient(Config.contentApiKey)
  lazy val content = Agent[Seq[MasterclassData]](Seq.empty)

  protected def getAllContent: Future[Seq[MasterclassData]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- getContent(nextPage)
        } yield {
          val masterclassData = response.results.flatMap(MasterclassDataExtractor.extractEventbriteInformation)
          val pagination = ContentAPIPagination(response.currentPage.getOrElse(0), response.pages.getOrElse(0))
          Some(pagination.nextPageOpt, masterclassData)

        }
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  def getContent(page: Int): Future[ItemResponse] = {
    val date = new DateTime(2014, 1, 1, 0, 0)
    contentApi.item.itemId("guardian-masterclasses")
      .fromDate(date)
      .pageSize(100)
      .page(page)
      .showReferences("eventbrite")
      .showFields("body")
      .showElements("image")
      .response.andThen {
      case Failure(GuardianContentApiError(status, message)) =>
        ContentApiMetrics.putResponseCode(status, "GET content")
        Logger.error(s"Error response from Content API $status")
    }
  }
}

object MasterclassDataService extends MasterclassDataService {
  val contentTask = ScheduledTask[Seq[MasterclassData]]("MasterclassDataService", Nil, 2.seconds, 2.minutes)(getAllContent)

  def getData(eventId: String) = contentTask.get().find(mc => mc.eventId.equals(eventId))
}

object MasterclassDataExtractor {

  val eventbriteUrl = "https?://www.eventbrite.co.uk/[^?\"]+"
  val regex = new Regex(eventbriteUrl)

  def extractEventbriteInformation(content: Content): Seq[MasterclassData] = {
    val elementOpt = content.elements.flatMap(_.find(_.relation == "main"))
    val assets = elementOpt.map(_.assets).getOrElse(List.empty)

    val eventbriteIdsFromRefs =
      content.references.filter(_.`type` == "eventbrite").map(_.id.stripPrefix("eventbrite/"))

    val eventbriteIds =
      if (eventbriteIdsFromRefs.nonEmpty) eventbriteIdsFromRefs else scrapeEventbriteIdsFrom(content)

    eventbriteIds.map(eventId => MasterclassData(eventId, content.webUrl, assets))
  }

  def scrapeEventbriteIdsFrom(content: Content): Seq[String] = for {
    body <- content.fields.map(_("body")).toSeq
    eventId <- regex.findAllIn(body).map(_.split("-").last)
  } yield eventId
}
