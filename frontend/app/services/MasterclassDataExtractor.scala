package services

import com.gu.contentapi.client.model.{Asset, Content}

import scala.util.matching.Regex

//todo refactor this to use Content and not webUrl and images
case class MasterclassData(eventId: String, webUrl: String, images: List[Asset])

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
