package model

import configuration.Config
import model.Eventbrite.EBEvent
import services.MasterclassData

object RichEvent {
  case class Metadata(
    identifier: String,
    title: String,
    shortTitle: String,
    eventListUrl: String,
    termsUrl: String,
    largeImg: Boolean,
    highlightsUrlOpt: Option[String]
  )

  val guLiveMetadata = Metadata("guardian-live", "Guardian Live events", "Events", controllers.routes.Event.list.url,
    Config.guardianLiveEventsTermsUrl, largeImg=true, Some(Config.guardianMembershipUrl + "#video"))
  val masterclassMetadata = Metadata("masterclasses", "Guardian Masterclasses", "Masterclasses",
    controllers.routes.Event.masterclasses.url, Config.guardianMasterclassesTermsUrl, largeImg=false, None)

  case class EventImage(assets: List[Grid.Asset], metadata: Grid.Metadata)

  trait RichEvent {
    val event: EBEvent
    val imgUrl: String
    val socialImgUrl: String
    val tags: Seq[String]

    val metadata: Metadata

    val imageMetadata: Option[Grid.Metadata]

    val availableWidths: String
    val fallbackImage = views.support.Asset.at("images/event-placeholder.gif")
  }

  case class GuLiveEvent(event: EBEvent, image: Option[EventImage]) extends RichEvent {
    val imgUrl = image.flatMap(_.assets.headOption).fold(fallbackImage) { asset =>
      val file = asset.secureFile.getOrElse(asset.file)
      val regex = "\\d+.jpg".r
      regex.replaceFirstIn(file, "{width}.jpg")
    }

    private val widths = image.fold(List.empty[Int])(_.assets.map(_.dimensions.width))

    val availableWidths = widths.mkString(",")

    val imageMetadata = image.map(_.metadata)

    val socialImgUrl = image.flatMap(_.assets.find(_.dimensions.width == widths.max)).fold(fallbackImage)(_.file)

    val tags = Nil

    val metadata = guLiveMetadata
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

