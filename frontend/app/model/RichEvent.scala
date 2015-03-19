package model

import com.gu.contentapi.client.model.{Asset, Content}
import configuration.Config
import model.Eventbrite.EBEvent
import services.MasterclassData

object RichEvent {
  case class Metadata(
    identifier: String,
    title: String,
    shortTitle: String,
    pluralTitle: String,
    description: Option[String],
    eventListUrl: String,
    termsUrl: String,
    largeImg: Boolean,
    preSale: Boolean,
    highlightsOpt: Option[HighlightsMetadata] = None,
    chooseTier: ChooseTierMetadata
  )

  case class ChooseTierMetadata(title: String, sectionTitle: String)
  case class HighlightsMetadata(title: String, url: String)

  val guLiveMetadata = Metadata(
    identifier="guardian-live",
    title="Guardian Live events",
    shortTitle="Events",
    pluralTitle="Guardian Live events",
    description=Some("""
      |Guardian Live is a programme of discussions, debates, interviews, keynote speeches and festivals.
      |Members can attend events that take the power of open journalism from print and digital into live experiences.
    """.stripMargin),
    eventListUrl=controllers.routes.Event.list.url,
    termsUrl=Config.guardianLiveEventsTermsUrl,
    largeImg=true,
    preSale=true,
    chooseTier=ChooseTierMetadata(
      "Guardian Live events are exclusively for Guardian members",
      "Choose a membership tier to continue with your booking"
    )
  )

  val masterclassMetadata = Metadata(
    identifier="masterclasses",
    title="Guardian Masterclasses",
    shortTitle="Masterclasses",
    pluralTitle="Masterclasses",
    description=Some("""
      |Guardian Masterclasses offer a broad range of short and long courses across a variety of disciplines from creative writing,
      | journalism, photography and design, film and digital media, music and cultural appreciation.
    """.stripMargin),
    eventListUrl=controllers.routes.Event.masterclasses.url,
    termsUrl=Config.guardianMasterclassesTermsUrl,
    largeImg=false,
    preSale=false,
    chooseTier=ChooseTierMetadata(
      "Choose a membership tier to continue with your booking",
      "Become a Partner or Patron to save 20% on your masterclass"
    )
  )

  val discoverMetadata = Metadata(
    identifier="discover",
    title="Guardian Discover",
    shortTitle="Events",
    pluralTitle="Discover events",
    description=None,
    eventListUrl=controllers.routes.Event.list.url,
    termsUrl=Config.guardianLiveEventsTermsUrl,
    largeImg=true,
    preSale=true,
    chooseTier=ChooseTierMetadata(
      "Guardian Discover events are exclusively for Guardian members",
      "Choose a membership tier to continue with your booking"
    )
  )

  case class GridImage(assets: List[Grid.Asset], metadata: Grid.Metadata)

  trait RichEvent {
    val event: EBEvent
    val imgOpt: Option[model.ResponsiveImageGroup]
    val socialImgUrl: Option[String]
    val tags: Seq[String]
    val metadata: Metadata
    val contentOpt: Option[Content]
    val pastImageOpt: Option[Asset]
    def deficientGuardianMembersTickets: Boolean
  }

  abstract class LiveEvent(image: Option[GridImage], contentOpt: Option[Content]) extends RichEvent {

    val imgOpt = image.map(ResponsiveImageGroup(_))

    val socialImgUrl = imgOpt.map(_.defaultImage)

    val tags = Nil

    val fallbackHighlightsMetadata = HighlightsMetadata("Watch highlights of past events",
      Config.guardianMembershipUrl + "#video")

    val highlight = contentOpt.map(c => HighlightsMetadata("Read more about this event", c.webUrl))
      .orElse(Some(fallbackHighlightsMetadata))

    val pastImageOpt = for {
      content <- contentOpt
      elements <- content.elements
      element <- elements.find(_.relation == "main")
      assetOpt <- element.assets.find(_.typeData.get("width") == Some("460"))
    } yield assetOpt

    def deficientGuardianMembersTickets = event.internalTicketing.flatMap(_.memberDiscountOpt).exists(_.fewerMembersTicketsThanGeneralTickets)
  }

  case class GuLiveEvent(event: EBEvent, image: Option[GridImage], contentOpt: Option[Content])
    extends LiveEvent(image, contentOpt) {
    val metadata = {
      guLiveMetadata.copy(highlightsOpt = highlight)
    }
  }

  case class DiscoverEvent(event: EBEvent, image: Option[GridImage], contentOpt: Option[Content])
    extends LiveEvent(image, contentOpt) {
    val metadata = {
      discoverMetadata.copy(highlightsOpt = highlight)
    }
  }

  case class MasterclassEvent(event: EBEvent, data: Option[MasterclassData]) extends RichEvent {
    val imgOpt = data.flatMap(_.images)
    val socialImgUrl = imgOpt.map(_.defaultImage)
    val tags = event.description.map(_.html).flatMap(MasterclassEvent.extractTags).getOrElse(Nil)
    val metadata = masterclassMetadata
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

