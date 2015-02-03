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
    chooseTier=ChooseTierMetadata(
      "Guardian Discover events are exclusively for Guardian members",
      "Choose a membership tier to continue with your booking"
    )
  )

  case class EventImage(assets: List[Grid.Asset], metadata: Grid.Metadata)

  trait RichEvent {
    val event: EBEvent
    val imgUrl: String
    val socialImgUrl: String
    val tags: Seq[String]

    val metadata: Metadata

    val imageMetadata: Option[Grid.Metadata]
    val contentOpt: Option[Content]
    val availableWidths: String
    val fallbackImage = views.support.Asset.at("images/event-placeholder.gif")
    val pastImageOpt: Option[Asset]
  }

  case class GuLiveEvent(event: EBEvent, image: Option[EventImage], contentOpt: Option[Content]) extends RichEvent {
    val imgUrl = image.flatMap(_.assets.headOption).fold(fallbackImage) { asset =>
      val file = asset.secureUrl.getOrElse(asset.file)
      val regex = "\\d+.jpg".r
      regex.replaceFirstIn(file, "{width}.jpg")
    }

    private val widths = image.fold(List.empty[Int])(_.assets.map(_.dimensions.width))

    val pastImageOpt = for {
      content <- contentOpt
      elements <- content.elements
      element <- elements.find(_.relation == "main")
      assetOpt <- element.assets.find(_.typeData.get("width") == Some("460"))
    } yield assetOpt

    val availableWidths = widths.mkString(",")

    val imageMetadata = image.map(_.metadata)

    val socialImgUrl = image.flatMap(_.assets.find(_.dimensions.width == widths.max)).fold(fallbackImage){ asset =>
      asset.secureUrl.getOrElse(asset.file)
    }

    val tags = Nil

    val metadata = {
      val fallbackHighlightsMetadata = HighlightsMetadata("Watch highlights of past events",
        Config.guardianMembershipUrl + "#video")
      val highlight = contentOpt.map(c => HighlightsMetadata("Read more about this event", c.webUrl))
        .orElse(Some(fallbackHighlightsMetadata))
      guLiveMetadata.copy(highlightsOpt = highlight)
    }

  }

  case class MasterclassEvent(event: EBEvent, data: Option[MasterclassData]) extends RichEvent {
    val imgUrl = data.flatMap(_.images.headOption).flatMap(_.file)
      .getOrElse(fallbackImage)
      .replace("http://static", "https://static-secure")


    val availableWidths = ""

    val socialImgUrl = imgUrl

    val imageMetadata = None
    val tags = event.description.map(_.html).flatMap(MasterclassEvent.extractTags).getOrElse(Nil)

    val metadata = masterclassMetadata

    val contentOpt = None
    val pastImageOpt = None
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

