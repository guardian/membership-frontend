package model

import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.Content
import com.gu.salesforce.Tier
import configuration.Links
import controllers.routes
import model.EventMetadata.{HighlightsMetadata, Metadata}
import model.Eventbrite.{EBTicketClass, EBOrder, EBEvent}
import org.joda.time.LocalDate
import services.MasterclassData
import utils.StringUtils._

import scala.collection.immutable.SortedMap

object RichEvent {

  // Used for arbitrary groupings of events with custom titles
  case class EventGroup(sequenceTitle: String, events: Seq[RichEvent])

  case class EventBrandCollection(
    live: Seq[RichEvent],
    local: Seq[RichEvent],
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
    events.sortWith(_.event.start < _.event.start)
  }

  def groupEventsByDay(events: Seq[RichEvent])(implicit ordering: Ordering[LocalDate]): SortedMap[LocalDate, Seq[RichEvent]] = {
    SortedMap(events.groupBy(_.start.toLocalDate).toSeq :_*)(ordering)
  }

  def groupEventsByDayAndMonth(events: Seq[RichEvent])(implicit ordering: Ordering[LocalDate]): SortedMap[LocalDate, SortedMap[LocalDate, Seq[RichEvent]]] = {
    SortedMap(groupEventsByDay(events)(ordering).groupBy(_._1.withDayOfMonth(1)).toSeq :_*)(ordering)
  }

  def groupEventsByMonth(events: Seq[RichEvent]): SortedMap[LocalDate, Seq[RichEvent]] = {
    SortedMap(events.groupBy(_.start.toLocalDate.withDayOfMonth(1)).toSeq :_*)
  }

  def getCitiesWithCount(events: Seq[RichEvent]): Seq[(String, Int)] = {
    val cities = events.flatMap(_.venue.address.flatMap(_.city))
    cities.groupBy(identity).mapValues(_.size).toSeq.sortBy{ case (name, size) => name }
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

    def countComplimentaryTicketsInOrder(order: EBOrder): Int = {
      val ticketIds = event.internalTicketing.map(_.complimentaryTickets).getOrElse(Nil).map(_.id)
      order.attendees.count(attendee => ticketIds.contains(attendee.ticket_class_id))
    }

    def retrieveDiscountedTickets(tier: Tier): Seq[EBTicketClass] = {
      val tickets = for {
        ticketing <- event.internalTicketing
        _ <- ticketing.memberDiscountOpt
        if Benefits.DiscountTicketTiers.contains(tier)
      } yield ticketing.memberBenefitTickets

      tickets.getOrElse(Seq[EBTicketClass]())
    }

    def isBookableByTier(tier: Tier): Boolean = this.internalTicketing.exists(_.salesDates.tierCanBuyTicket(tier))
  }

  abstract class LiveEvent(
    image: Option[GridImage],
    contentOpt: Option[Content]
  ) extends RichEvent {
    val detailsUrl = routes.Event.details(event.slug).url
    val hasLargeImage = true
    val canHavePriorityBooking = true
    val imgOpt = image.flatMap(ResponsiveImageGroup.fromGridImage)
    val socialImgUrl = imgOpt.map(_.defaultImage)
    val pastImageOpt = contentOpt.flatMap(ResponsiveImageGroup.fromContent)
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

