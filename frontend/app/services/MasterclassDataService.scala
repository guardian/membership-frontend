package services

import akka.agent.Agent
import com.gu.contentapi.client.model.{Asset, Content}
import play.api.libs.iteratee.{Enumerator, Iteratee}
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.matching.Regex

case class MasterclassData(eventId: String, webUrl: String, images: List[Asset])

case class MasterclassResponse[T](pagination: ContentAPIPagination, data: Seq[T])


trait MasterclassDataService extends GuardianContent {

  protected def getAllContent: Future[Seq[MasterclassData]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- masterclasses(nextPage)
        } yield {
          val masterclassData = response.results.flatMap(MasterclassDataExtractor.extractEventbriteInformation)
          val pagination = ContentAPIPagination(response.currentPage.getOrElse(0), response.pages.getOrElse(0))
          Some(pagination.nextPageOpt, masterclassData)

        }
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  val contentTask = ScheduledTask[Seq[MasterclassData]]("MasterclassDataService", Nil, 2.seconds, 2.minutes)(getAllContent)

  def getData(eventId: String) = contentTask.get().find(mc => mc.eventId.equals(eventId))
}

object MasterclassDataService extends MasterclassDataService

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
