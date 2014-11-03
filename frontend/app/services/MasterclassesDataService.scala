package services

import akka.agent.Agent
import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.{Asset, Content, ItemResponse}
import configuration.Config
import org.joda.time.DateTime
import play.api.libs.iteratee.{Enumerator, Iteratee}
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex

case class MasterclassesData(eventId: String, webUrl: String, images: List[Asset])

case class MasterclassResponse[T](pagination: ContentAPIPagination, data: Seq[T])

case class ContentAPIPagination(currentPage: Int, pages: Int) {
  lazy val nextPageOpt = Some(currentPage + 1).filter(_ <= pages)
}

trait MasterclassesDataService {

  val contentApi = new GuardianContentClient(Config.contentApiKey)
  lazy val content = Agent[Seq[MasterclassesData]](Seq.empty)

  private def getAllContent: Future[Seq[MasterclassesData]] = {
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
    val date = new DateTime(2014, 1, 1)
    contentApi.item.itemId("guardian-masterclasses")
      .fromDate(date)
      .pageSize(100)
      .page(page)
      .showFields("body")
      .showElements("image")
      .response
  }
}

object MasterclassDataExtractor {

  val eventbriteUrl = "https://www.eventbrite.co.uk/[^\"]+"
  val regex = new Regex(eventbriteUrl)

  def extractEventbriteInformation(content: Content): List[MasterclassesData] = {

    val element = content.elements.flatMap(elements => elements.find(_.relation == "main"))
    val assets = element.map(_.assets).getOrElse(List.empty)

    val bodyOpt = content.fields.map(_("body"))

    bodyOpt.map { body =>
      for (eventUrls <- regex.findAllIn(body).toList) yield {
        val eventId = eventUrls.split("-").last
        MasterclassesData(eventId, content.webUrl, assets)

      }
    }.getOrElse(Nil)
  }
}

object MasterclassesDataService extends MasterclassesDataService with ScheduledTask[Seq[MasterclassesData]]{

  val initialValue = Nil
  val interval = 2.minutes
  val initialDelay = 10.seconds

  def refresh(): Future[Seq[MasterclassesData]] = getAllContent

  def masterclassesData: Seq[MasterclassesData] = agent.get()
}