package model

import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.Content
import com.gu.memsub.images.Grid.Asset
import com.gu.memsub.images.{Grid, _}
import com.gu.salesforce.Tier
import configuration.Links
import controllers.routes
import model.EventMetadata.{HighlightsMetadata, Metadata}
import model.Eventbrite.{EBOrder, EBTicketClass, EventWithDescription}
import model.Footer.{LiveFooter, MasterClassesFooter}
import model.Header.{LiveHeader, MasterClassesHeader}
import org.joda.time.LocalDate
import services.MasterclassData
import utils.StringUtils._

import scala.collection.immutable.SortedMap

object RichEvent {

  // Used for arbitrary groupings of events with custom titles
  case class EventGroup(sequenceTitle: String, events: Seq[RichEvent])

  case class EventBrandCollection(
    live: Seq[RichEvent],
    masterclasses: Seq[RichEvent]
  )

  case class CalendarMonthDayGroup(
    title: String,
    list: SortedMap[LocalDate, SortedMap[LocalDate, Seq[RichEvent]]]
  ) {
    val length = list.flatMap(_._2).flatMap(_._2).toSeq.length
  }

  case class FilterItem(name: String, count: Int) {
    var slug = slugify(name)
  }

  def chronologicalSort(events: Seq[RichEvent]) = {
    events.sortWith(_.underlying.ebEvent.start < _.underlying.ebEvent.start)
  }

  def groupEventsByDay(events: Seq[RichEvent])(implicit ordering: Ordering[LocalDate]): SortedMap[LocalDate, Seq[RichEvent]] = {
    SortedMap(events.groupBy(_.underlying.ebEvent.start.toLocalDate).toSeq :_*)(ordering)
  }

  def groupEventsByDayAndMonth(events: Seq[RichEvent])(implicit ordering: Ordering[LocalDate]): SortedMap[LocalDate, SortedMap[LocalDate, Seq[RichEvent]]] = {
    SortedMap(groupEventsByDay(events)(ordering).groupBy(_._1.withDayOfMonth(1)).toSeq :_*)(ordering)
  }

  def groupEventsByMonth(events: Seq[RichEvent]): SortedMap[LocalDate, Seq[RichEvent]] = {
    SortedMap(events.groupBy(_.underlying.ebEvent.start.toLocalDate.withDayOfMonth(1)).toSeq :_*)
  }

  def getCitiesWithCount(events: Seq[RichEvent]): Seq[(String, Int)] = {
    val cities = events.flatMap(_.underlying.ebEvent.venue.address.flatMap(_.city))
    cities.groupBy(identity).view.mapValues(_.size).toSeq.sortBy{ case (name, size) => name }
  }

  case class GridImage(assets: List[Grid.Asset], metadata: Grid.Metadata, master: Option[Asset]) {
    lazy val masterOrBestAsset = master orElse assets.sortBy(_.pixels).reverse.headOption
  }

  trait RichEvent {
    val underlying: EventWithDescription
    val detailsUrl: String
    val logoOpt: Option[ProviderLogo]
    val imgOpt: Option[ResponsiveImageGroup]
    val socialImgUrl: Option[String]
    val gridImgUrl: Option[String]
    val header: Header = LiveHeader
    val footer: Footer = LiveFooter
    val schema: EventSchema
    val tags: Seq[String]
    val metadata: Metadata
    val contentOpt: Option[Content]
    val pastImageOpt: Option[ResponsiveImageGroup]
    val hasLargeImage: Boolean
    val canHavePriorityBooking: Boolean
    def deficientGuardianMembersTickets: Boolean

    def countComplimentaryTicketsInOrder(order: EBOrder): Int = {
      val ticketIds = underlying.internalTicketing.map(_.complimentaryTickets).getOrElse(Nil).map(_.id)
      order.attendees.count(attendee => ticketIds.contains(attendee.ticket_class_id))
    }

    def retrieveDiscountedTickets(tier: Tier): Seq[EBTicketClass] = {
      val tickets = for {
        ticketing <- underlying.internalTicketing
        _ <- ticketing.memberDiscountOpt
        if Benefits.DiscountTicketTiers.contains(tier)
      } yield ticketing.memberBenefitTickets

      tickets.getOrElse(Seq[EBTicketClass]())
    }

    def isBookableByTier(tier: Tier): Boolean = underlying.internalTicketing.exists(_.salesDates.tierCanBuyTicket(tier))
  }

  abstract class LiveEvent(
    image: Option[GridImage],
    contentOpt: Option[Content]
  ) extends RichEvent {
    val detailsUrl = routes.Event.detailsLive(underlying.ebEvent.slug).url
    val hasLargeImage = true
    val canHavePriorityBooking = true
    val imgOpt = image.flatMap(model.ResponsiveImageGroup.fromGridImage)
    val socialImgUrl = imgOpt.map(_.defaultImage.toString)
    val pastImageOpt = contentOpt.flatMap(model.ResponsiveImageGroup.fromContent)
    val schema = EventSchema.from(this)
    val tags = Nil

    override val gridImgUrl = for {
      i <- image
      best <- i.masterOrBestAsset
      uri <- best.secureUrl
    } yield uri

    val fallbackHighlightsMetadata = HighlightsMetadata("View highlights of past events",
      Links.membershipFront + "#recent-events")
    val highlight = contentOpt.map(c => HighlightsMetadata("Read more about this event", c.webUrl))
      .orElse(Some(fallbackHighlightsMetadata))

    def deficientGuardianMembersTickets = {
      underlying.internalTicketing
        .flatMap(_.memberDiscountOpt)
        .exists(_.fewerMembersTicketsThanGeneralTickets)
    }
  }

  case class GuLiveEvent(
    underlying: EventWithDescription,
    image: Option[GridImage],
    contentOpt: Option[Content]
  ) extends LiveEvent(image, contentOpt) {
    val metadata = EventMetadata.liveMetadata.copy(highlightsOpt = highlight)
    val logoOpt = Some(ProviderLogo(this))
  }

  case class MasterclassEvent(
    underlying: EventWithDescription,
    data: Option[MasterclassData]
  ) extends RichEvent {
    val detailsUrl = routes.Event.detailsMasterclass(underlying.ebEvent.slug).url
    val hasLargeImage = false
    val canHavePriorityBooking = false
    val imgOpt = data.flatMap(_.images)
    val socialImgUrl = imgOpt.map(_.defaultImage.toString)
    val schema = EventSchema.from(this)
    val tags = MasterclassEvent.extractTags(underlying.ebDescription.description).getOrElse(Nil)
    val metadata = EventMetadata.masterclassMetadata
    val logoOpt = Some(ProviderLogo(this))
    override val header = MasterClassesHeader
    override val footer: Footer = MasterClassesFooter
    val contentOpt = None
    val pastImageOpt = None
    override val gridImgUrl = None
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

}

