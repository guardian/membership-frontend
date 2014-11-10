package services

import akka.agent.Agent
import com.gu.contentapi.client.{GuardianContentApiError, GuardianContentClient}
import com.gu.contentapi.client.model.{Asset, Content, ItemResponse}
import configuration.Config
import monitoring.ContentApiMetrics
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex

case class MasterclassData(eventId: String, webUrl: String, images: List[Asset])

case class MasterclassResponse[T](pagination: ContentAPIPagination, data: Seq[T])

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

trait MasterclassDataService {

  val contentApi = new GuardianContentClient(Config.contentApiKey)
  lazy val content = Agent[Seq[MasterclassData]](Seq.empty)

  private def getAllContent: Future[Seq[MasterclassData]] = {
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
      .showFields("body")
      .showElements("image")
      .response.recover {
        case GuardianContentApiError(status, message) => {
          ContentApiMetrics.putResponseCode(status, "GET content")
          Logger.error(s"Error response from Content API $status")
          throw GuardianContentApiError(status, message)
        }
      }
  }
}

object MasterclassDataService extends MasterclassDataService with ScheduledTask[Seq[MasterclassData]]{
  def getData(eventId: String) = masterclassData.find(mc => mc.eventId.equals(eventId))

  val initialValue = Nil
  val interval = 60.seconds
  val initialDelay = 2.seconds

  def refresh(): Future[Seq[MasterclassData]] = getAllContent

  def masterclassData: Seq[MasterclassData] = agent.get()
}

object MasterclassDataExtractor {

  val eventbriteUrl = "https?://www.eventbrite.co.uk/[^\"]+"
  val regex = new Regex(eventbriteUrl)

  def extractEventbriteInformation(content: Content): List[MasterclassData] = {

    val element = content.elements.flatMap(elements => elements.find(_.relation == "main"))
    val assets = element.map(_.assets).getOrElse(List.empty)

    val bodyOpt = content.fields.map(_("body"))

    bodyOpt.map { body =>
      val eventUrls = regex.findAllIn(body).toList
      eventUrls.map { eventUrl =>
        val eventId = eventUrl.split("-").last
        MasterclassData(eventId, content.webUrl, assets)
      }
    }.getOrElse(Nil)
  }
}