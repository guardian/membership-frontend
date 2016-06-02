package services

import com.gu.contentapi.client.model.v1.{Asset, Content}
import com.gu.memsub.images.ResponsiveImageGroup

import scala.util.matching.Regex

//todo refactor this to use Content and not webUrl and images
case class MasterclassData(eventId: String, webUrl: String, images: Option[ResponsiveImageGroup])

object MasterclassDataExtractor {

  val eventbriteUrl = "https?://www.eventbrite.co.uk/[^?\"]+"
  val regex = new Regex(eventbriteUrl)

  def extractEventbriteInformation(content: Content): Seq[MasterclassData] = {
    val eventbriteIdsFromRefs =
      content.references.filter(_.`type` == "eventbrite").map(_.id.stripPrefix("eventbrite/"))

    val eventbriteIds = (eventbriteIdsFromRefs ++ scrapeEventbriteIdsFrom(content)).distinct

    eventbriteIds.map(eventId => MasterclassData(eventId, content.webUrl, model.ResponsiveImageGroup.fromContent(content)))
  }

  def scrapeEventbriteIdsFrom(content: Content): Seq[String] = for {
    body <- content.fields.flatMap(_.body).toSeq
    eventId <- regex.findAllIn(body).map(_.split("-").last)
  } yield eventId
}
