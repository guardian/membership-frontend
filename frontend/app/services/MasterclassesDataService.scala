package services

import akka.agent.Agent
import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.{ItemResponse, Asset, Content}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Enumerator, Iteratee}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex
import configuration.Config
import play.api.Play.current

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

object MasterclassesDataService extends MasterclassesDataService {

  def start() {
    def scheduleAgentRefresh[T](agent: Agent[T], refresher: => Future[T], intervalPeriod: FiniteDuration) = {
      Akka.system.scheduler.schedule(1.second, intervalPeriod) {
        agent.sendOff(_ => Await.result(refresher, 15.seconds))
      }
    }
    Logger.info("Starting EventbriteService background tasks")
    scheduleAgentRefresh(content, getAllContent, 1.minute)

  }
}