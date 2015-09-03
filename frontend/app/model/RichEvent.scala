package model

import com.gu.contentapi.client.model.Content
import configuration.Links
import controllers.routes
import model.EventMetadata.{HighlightsMetadata, Metadata}
import model.Eventbrite.EBEvent
import services.MasterclassData

object RichEvent {

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

