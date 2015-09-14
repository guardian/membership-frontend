package model

import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.Content
import configuration.Links
import controllers.routes
import model.EventMetadata.{HighlightsMetadata, Metadata}
import model.Eventbrite.EBEvent
import services.MasterclassData

import scala.collection.immutable.SortedMap

object RichEvent {

  def groupEventsByDay(events: Seq[RichEvent])(implicit chronologicalOrdering: Ordering[LocalDate]): SortedMap[LocalDate, Seq[RichEvent]] = {
    val unsortedMap = events.groupBy(_.start.toLocalDate)
    SortedMap(unsortedMap.toSeq :_*)(chronologicalOrdering)
  }

  def groupEventsByDayAndMonth(events: Seq[RichEvent])(implicit chronologicalOrdering: Ordering[LocalDate]):  SortedMap[LocalDate, SortedMap[LocalDate, Seq[RichEvent]]] = {
    val unsortedMap = groupEventsByDay(events)(chronologicalOrdering).groupBy(_._1.withDayOfMonth(1))
    SortedMap(unsortedMap.toSeq :_*)(chronologicalOrdering)
  }

  def groupEventsByMonth(events: Seq[RichEvent]): SortedMap[LocalDate, Seq[RichEvent]] = {
    val unsortedMap = events.groupBy(_.start.toLocalDate.withDayOfMonth(1)).map {
      case (monthDate, eventsForMonth) => monthDate -> eventsForMonth
    }
    SortedMap(unsortedMap.toSeq :_*)
  }

  case class GridImage(assets: List[Grid.Asset], metadata: Grid.Metadata)

  trait RichEvent {
    val event: EBEvent
    val detailsUrl: String
    val logoOpt: Option[ProviderLogo]
    val imgOpt: Option[model.ResponsiveImageGroup]
    val socialImgUrl: Option[String]
    val schema: EventSchema
    val tags: Seq[String]
    val metadata: Metadata
    val contentOpt: Option[Content]
    val pastImageOpt: Option[ResponsiveImageGroup]
    val hasLargeImage: Boolean
    val canHavePriorityBooking: Boolean
    def deficientGuardianMembersTickets: Boolean
  }

  abstract class LiveEvent(
    image: Option[GridImage],
    contentOpt: Option[Content]
  ) extends RichEvent {
    val detailsUrl = routes.Event.details(event.slug).url
    val hasLargeImage = true
    val canHavePriorityBooking = true
    val imgOpt = image.map(ResponsiveImageGroup(_))
    val socialImgUrl = imgOpt.map(_.defaultImage)
    val pastImageOpt = contentOpt.flatMap(ResponsiveImageGroup(_))
    val schema = EventSchema.from(this)
    val tags = Nil

    val fallbackHighlightsMetadata = HighlightsMetadata("View highlights of past events",
      Links.membershipFront + "#recent-events")
    val highlight = contentOpt.map(c => HighlightsMetadata("Read more about this event", c.webUrl))
      .orElse(Some(fallbackHighlightsMetadata))

    def deficientGuardianMembersTickets = {
      event.internalTicketing
        .flatMap(_.memberDiscountOpt)
        .exists(_.fewerMembersTicketsThanGeneralTickets)
    }
  }

  case class GuLiveEvent(
    event: EBEvent,
    image: Option[GridImage],
    contentOpt: Option[Content]
  ) extends LiveEvent(image, contentOpt) {
    val metadata = EventMetadata.liveMetadata.copy(highlightsOpt = highlight)
    val logoOpt = Some(ProviderLogo(this))
  }

  case class LocalEvent(
    event: EBEvent,
    image: Option[GridImage],
    contentOpt: Option[Content]
  ) extends LiveEvent(image, contentOpt) {
    val metadata = EventMetadata.localMetadata.copy(highlightsOpt = highlight)
    val logoOpt = Some(ProviderLogo(this))
  }

  case class MasterclassEvent(
    event: EBEvent,
    data: Option[MasterclassData]
  ) extends RichEvent {
    val detailsUrl = routes.Event.details(event.slug).url
    val hasLargeImage = false
    val canHavePriorityBooking = false
    val imgOpt = data.flatMap(_.images)
    val socialImgUrl = imgOpt.map(_.defaultImage)
    val schema = EventSchema.from(this)
    val tags = event.description.map(_.html).flatMap(MasterclassEvent.extractTags).getOrElse(Nil)
    val metadata = EventMetadata.masterclassMetadata
    val logoOpt = Some(ProviderLogo(this))
    val contentOpt = None
    val pastImageOpt = None
    def deficientGuardianMembersTickets = false
  }

  object MasterclassEvent {
    case class tagItem(categoryName: String, subCategories: Seq[String] = Seq())

    val tags = Seq(
      tagItem("Writing", Seq("Copywriting", "Creative writing", "Research", "Fiction", "Non-fiction")),
      tagItem("Publishing"),
      tagItem("Journalism"),
      tagItem("Business"),
      tagItem("Digital"),
      tagItem("Culture"),
      tagItem("Food and drink"),
      tagItem("Media")
    )

    // if a tag is hyphenated (Non-fiction) then weirdness/duplication happens here
    // so we replace it with an underscore in URLs (ugly, but limited alternatives)
    def encodeTag(tag: String) = tag.toLowerCase.replace("-", "_").replace(" ", "-")
    def decodeTag(tag: String) = tag.capitalize.replace("-", " ").replace("_", "-")

    def extractTags(s: String): Option[Seq[String]] =
      "<!--\\s*tags:(.*?)-->".r.findFirstMatchIn(s).map(_.group(1).split(",").toSeq.map(_.trim.toLowerCase))
  }

  implicit def eventToEBEvent(event: RichEvent): EBEvent = event.event

}

