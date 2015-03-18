package services

import com.gu.contentapi.client.model.{Asset, Content}
import model.ResponsiveImageGroup

import scala.util.matching.Regex

//todo refactor this to use Content and not webUrl and images
case class MasterclassData(eventId: String, webUrl: String, images: Option[ResponsiveImageGroup])

object MasterclassDataExtractor {

  val eventbriteUrl = "https?://www.eventbrite.co.uk/[^?\"]+"
  val regex = new Regex(eventbriteUrl)

  def extractEventbriteInformation(content: Content): Seq[MasterclassData] = {
    val eventbriteIdsFromRefs =
      content.references.filter(_.`type` == "eventbrite").map(_.id.stripPrefix("eventbrite/"))

    val eventbriteIds =
      if (eventbriteIdsFromRefs.nonEmpty) eventbriteIdsFromRefs else scrapeEventbriteIdsFrom(content)

    eventbriteIds.map(eventId => MasterclassData(eventId, content.webUrl, ResponsiveImageGroup(content)))
  }

  def scrapeEventbriteIdsFrom(content: Content): Seq[String] = for {
    body <- content.fields.map(_("body")).toSeq
    eventId <- regex.findAllIn(body).map(_.split("-").last)
  } yield eventId
}
