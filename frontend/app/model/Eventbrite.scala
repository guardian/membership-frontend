package model

import com.github.nscala_time.time.Imports._
import com.gu.i18n.Currency
import com.gu.i18n.Currency.GBP
import com.gu.memsub.Price
import com.gu.salesforce.Tier
import io.lemonlabs.uri.{Uri, Url}
import io.lemonlabs.uri.typesafe.dsl._
import configuration.Config
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import services.GridService
import services.GridService.{CropQueryParam, ImageIdWithCrop}
import utils.StringUtils._
import views.support.Asset
import views.support.Dates.YearMonthDayHours

import scala.util.{Failure, Success, Try}

object Eventbrite {

  // This can be deleted once all these events have completed
  val HiddenEvents = Map(
    "18862189316" -> "19222944344",
    "18882535171" -> "19223627387",
    "18882579303" -> "19223798900",
    "18882595351" -> "19223835008",
    "18882606384" -> "19223927284"
  )

  val googleMapsUri = Uri.parse("https://maps.google.com/").toUrl

  trait EBObject

  case class EBError(error: String, error_description: String, status_code: Int) extends Throwable with EBObject {
    override def getMessage: String = s"$status_code $error - $error_description"
  }

  case class EBResponse[T](pagination: EBPagination, data: Seq[T]) extends EBObject

  case class EBPagination(page_number: Int, page_count: Int) extends EBObject {
    lazy val nextPageOpt = Some(page_number + 1).filter(_ <= page_count)
  }

  case class EBRichText(text: String, html: String) {
    def cleanHtml: String = {
      val stylePattern = "(?i)style=(\".*?\"|'.*?'|[^\"'][^\\s]*)".r
      val cleanStyle = stylePattern replaceAllIn(html, "")
      val clean = "(?i)<br>".r.replaceAllIn(cleanStyle, "")

      // Remove Masterclass return URL
      val mcPattern = "(?i)<a[^>]+>Full course and returns information on the Masterclasses website</a>".r
      mcPattern.replaceAllIn(clean, "")
    }

    lazy val blurb = truncateToWordBoundary(text, 120)
  }

  case class EBAddress(
    address_1: Option[String],
    address_2: Option[String],
    city: Option[String],
    region: Option[String],
    postal_code: Option[String],
    country: Option[String]
  ) extends EBObject {
    lazy val toSeq: Seq[String] = Seq(address_1, address_2, city, region, postal_code).flatten
    lazy val asLine = seqToLine(toSeq)
  }

  case class EBVenue(
    address: Option[EBAddress],
    name: Option[String]
  ) extends EBObject {
    lazy val addressLine = address.flatMap(_.asLine)
    lazy val addressShortLine = seqToLine(Seq(name, address.flatMap(_.city)).flatten)
    lazy val addressDefaultLine = seqToLine(Seq(name, address.flatMap(_.city), address.flatMap(_.postal_code)).flatten)
    lazy val googleMapsLink: Option[String] = {
      addressLine.map(al => (googleMapsUri ? ("q" -> (name.map(_ + ", ").mkString + al))).toString())
    }
  }

  def penceToPounds(priceInPence: Double): Float = {
    priceInPence.round / 100f
  }

  def formatPrice(priceInPence: Double): String = {
    val priceInPounds = priceInPence.round / 100f
    if (priceInPounds.isWhole) f"$priceInPounds%.0f" else f"$priceInPounds%.2f"
  }

  def formatPriceWithCurrency(priceInPence: Double, currencyCode: String): String = {
    Currency.fromString(currencyCode).getOrElse(GBP).glyph + formatPrice(priceInPence)
  }

  case class EBPricing(value: Int, currency: String) extends EBObject {
    lazy val formattedPrice = formatPriceWithCurrency(value, currency)
    def add(other: EBPricing) : EBPricing = {
      if (currency != other.currency) {
        throw new UnsupportedOperationException("Attempted to add two EBPricing objects with different currencies")
      }
      EBPricing(value+other.value, currency)
    }
  }

  case class EventTimes(created: Instant, start: DateTime)

  /**
   * https://developer.eventbrite.com/docs/ticket-class-object/
   */
  case class EBTicketClass(id: String,
    name: String,
    description: Option[String],
    free: Boolean,
    quantity_total: Int,
    quantity_sold: Int,
    on_sale_status: Option[String], // Currently undocumented, so treating as optional. "SOLD_OUT", "AVAILABLE", "UNAVAILABLE"
    cost: Option[EBPricing],
    fee: Option[EBPricing],
    tax: Option[EBPricing],
    sales_end: Instant,
    sales_start: Option[Instant],
    hidden: Option[Boolean]) extends EBObject {
    val isHidden = hidden.contains(true)

    val isGuestList = isHidden && name.toLowerCase.contains("guestlist")

    val isMemberBenefit = isHidden && name.toLowerCase.startsWith("guardian member")
    val isComplimentary = isHidden && name.toLowerCase.startsWith("member's ticket at no extra cost")

    val isSoldOut = on_sale_status.contains("SOLD_OUT") || quantity_sold >= quantity_total

    val priceInPence = cost.map(_.value).getOrElse(0)
    val feeInPence = fee.map(_.value).getOrElse(0)
    val priceValue = formatPrice(priceInPence)
    val priceText = cost.map(_.formattedPrice).getOrElse("Free")
    val feeText = fee
      .filter(_.value > 0)
      .map(theFee => EBPricing(theFee.value + tax.map(_.value).getOrElse(0), theFee.currency))
      .map(_.formattedPrice)
    val totalCost = cost.map(c => c add fee.getOrElse(EBPricing(0, c.currency)))
    val currencyCode = cost.map(_.currency).getOrElse(GBP.iso)
  }

  sealed trait Ticketing

  case object ExternalTicketing extends Ticketing

  object InternalTicketing {
    def optFrom(event: EBEvent): Option[InternalTicketing] = {
      val allTickets = event.ticket_classes

      val generalReleaseTicketOpt = allTickets.find(!_.isHidden)
      val memberBenefitTickets = allTickets.filter(_.isMemberBenefit)
      val complimentaryTickets = allTickets.filter(_.isComplimentary)

      for (primaryTicket <- (generalReleaseTicketOpt ++ memberBenefitTickets).headOption) yield {
        InternalTicketing(event.times, primaryTicket, memberBenefitTickets, complimentaryTickets, allTickets, event.capacity)
      }
    }
  }

  case class InternalTicketing(
    eventTimes: EventTimes,
    primaryTicket: EBTicketClass,
    memberBenefitTickets: Seq[EBTicketClass],
    complimentaryTickets: Seq[EBTicketClass],
    allTickets: Seq[EBTicketClass],
    capacity: Int) extends Ticketing {

    require(allTickets.contains(primaryTicket))
    require(memberBenefitTickets.forall(allTickets.contains))

    val generalReleaseTicketOpt = Some(primaryTicket).filterNot(_.isMemberBenefit)

    val memberBenefitTicketOpt = memberBenefitTickets.headOption

    val ticketsSold = allTickets.map(_.quantity_sold).sum
    val ticketsNotSold = capacity - ticketsSold

    val isSoldOut = allTickets.forall(_.isSoldOut) || ticketsSold >= capacity

    val isFree = primaryTicket.free

    val salesDates = TicketSaleDates.datesFor(eventTimes, primaryTicket)

    val salesEnd = allTickets.map(_.sales_end).max

    val isCurrentlyAvailableToPaidMembersOnly =
      generalReleaseTicketOpt.map(!TicketSaleDates.datesFor(eventTimes, _).tierCanBuyTicket(Tier.friend)).getOrElse(true)

    val memberDiscountOpt = for {
      generalReleaseTicket <- generalReleaseTicketOpt
      memberTicket <- memberBenefitTicketOpt
    } yield DiscountBenefitTicketing(generalReleaseTicket, memberTicket)

    val isPossiblyMissingDiscount = !isFree && memberDiscountOpt.isEmpty

    val isPossiblyMissingComplimentaryTicket = !isFree && !complimentaryTickets.exists(_.free)

    val ticketsEndingSaleBeforeEvent =
      allTickets.filter(!_.isGuestList).groupBy(t => new Duration(t.sales_end, eventTimes.start).toPeriod().normalizedStandard(YearMonthDayHours)).filterKeys(_.standardDuration > 2.hours)
  }

  case class DiscountBenefitTicketing(generalRelease: EBTicketClass, member: EBTicketClass) {
    val saving = (generalRelease.priceInPence + generalRelease.feeInPence) - member.priceInPence
    val savingExcludingFee = generalRelease.priceInPence - member.priceInPence

    val savingText = formatPriceWithCurrency(saving, member.currencyCode)

    val roundedSavingPercentage: Int = math.round(100 * (saving.toFloat / generalRelease.priceInPence))
    val roundedSavingPercentageExcludingFee: Int = math.round(100 * (savingExcludingFee.toFloat / generalRelease.priceInPence))

    lazy val nonStandardSaving = roundedSavingPercentage != Config.roundedDiscountPercentage

    val isSoldOut = member.isSoldOut

    val fewerMembersTicketsThanGeneralTickets = member.quantity_total < generalRelease.quantity_total
  }

  case class EventWithDescription(ebEvent: EBEvent, ebDescription: EBDescription) {

    val ticketing: Option[Ticketing] =
      if (ebDescription.description.contains("<!-- noTicketEvent -->")) Some(ExternalTicketing) else InternalTicketing.optFrom(this.ebEvent)

    val internalTicketing: Option[InternalTicketing] = ticketing collect {
      case t: InternalTicketing => t
    }

    val generalReleaseTicket = for {
      ticketing <- internalTicketing
      ticket <- ticketing.generalReleaseTicketOpt
    } yield ticket

    val hasComplimentaryTickets = internalTicketing.exists(_.complimentaryTickets.nonEmpty)
    val isLimitedAvailability = internalTicketing.exists(event => event.ticketsNotSold <= Config.eventbriteLimitedAvailabilityCutoff && !event.isSoldOut)
    val ticketsNotSold = internalTicketing.map(_.ticketsNotSold)
    val isSoldOut = internalTicketing.exists(_.isSoldOut)

    val isBookable = {
      val isStartedAndHasBookableTicketClasses = ebEvent.status == "started" && ebEvent.ticket_classes.exists(_.sales_end < DateTime.now())
      (ebEvent.status == "live" || isStartedAndHasBookableTicketClasses) && !isSoldOut
    }

    val statusSchema: Option[String] = {
      if (ebEvent.isPastEvent) None
      else if (isSoldOut) Some("http://schema.org/OutOfStock")
      else if(isLimitedAvailability) Some("http://schema.org/LimitedAvailability")
      else Some("http://schema.org/InStock")
    }

    val statusText: Option[String] = {
      if (ebEvent.isPastEvent) Some("Past event")
      else if (isSoldOut) Some("Sold out")
      else if (ebEvent.status == "draft") Some("Preview of Draft Event")
      else None
    }

    val mainImageUrl: Option[Url] = for {
      m <- """\smain-image:\s*(.*?)\s""".r.findFirstMatchIn(ebDescription.description)
      uri <- Try(Uri.parse(m.group(1))) match {
        case Success(uri) => Some(uri.toUrl)
        case Failure(e) =>
          SafeLogger.error(scrub"Event ${ebEvent.id} - can't parse main-image url from text '${m.matched}'", e)
          None
      }
    } yield uri

    val mainImageHasNoCrop: Boolean = mainImageUrl.exists(!_.query.params.contains(CropQueryParam))

    val mainImageGridId: Option[ImageIdWithCrop] = mainImageUrl.flatMap(ImageIdWithCrop.fromGuToolsUri)

  }

  case class EBDescription(
    description: String
  ) extends EBObject {

    val providerOpt = for {
      m <- "<!-- provider: (\\S+) -->".r.findFirstMatchIn(description)
      providerOpt <- EBEvent.availableProviders.find(_.id == m.group(1))
    } yield providerOpt

    val isPartnerEvent = providerOpt.isDefined

    def cleanHtml: String = {
      val stylePattern = "(?i)style=(\".*?\"|'.*?'|[^\"'][^\\s]*)".r
      val cleanStyle = stylePattern replaceAllIn(description, "")
      val clean = "(?i)<br>".r.replaceAllIn(cleanStyle, "")

      // Remove Masterclass return URL
      val mcPattern = "(?i)<a[^>]+>Full course and returns information on the Masterclasses website</a>".r
      mcPattern.replaceAllIn(clean, "")
    }

  }

  case class EBEvent(
    name: EBRichText,
    description: Option[EBRichText],//summary, not the full description
    url: String,
    id: String,
    start: DateTime,
    end: DateTime,
    created: Instant,
    venue: EBVenue,
    capacity: Int,
    ticket_classes: Seq[EBTicketClass],
    status: String
  ) extends EBObject {

    val times = EventTimes(created, start)
    val startAndEnd = new Interval(start, end)

    val limitedAvailabilityText = "Last tickets remaining" //why is this here?
    val isPastEvent = {
      val conditions = Set("ended", "completed")
      conditions.contains(status)
    }

    val slug = slugify(name.text) + "-" + id

    lazy val memUrl = Config.membershipUrl + controllers.routes.Event.details(slug)
  }

  object EBEvent {

    val availableProviders = Seq(
      ProviderLogo("birkbeck", "Birkbeck", Asset.at("images/providers/birkbeck.svg")),
      ProviderLogo("idler", "Idler Academy", Asset.at("images/providers/idler.png")),
      ProviderLogo("csm", "Central Saint Martins", Asset.at("images/providers/csm.svg")),
      ProviderLogo("tpg", "The Photographers' Gallery", Asset.at("images/providers/tpg.svg")),
      ProviderLogo("5x15", "5x15", Asset.at("images/providers/5x15.png")),
      ProviderLogo("moa", "Museum of Architecture", Asset.at("images/providers/moa.png")),
      ProviderLogo("shubbak", "Shubbak Festival", Asset.at("images/providers/shubbak.svg")),
      ProviderLogo("british-council", "British Council", Asset.at("images/providers/british-council.svg"))
    )

    val expansions = Seq(
      "category",
      "subcategory",
      "format",
      "venue",
      "ticket_classes",
      "logo",
      "organizer"
    )

    def slugToId(slug: String): Option[String] = "-?(\\d+)$".r.findFirstMatchIn(slug).map(_.group(1))
  }

  trait EBCode extends EBObject {
    val code: String
    val quantity_available: Int
  }

  case class EBDiscount(code: String, quantity_available: Int, quantity_sold: Int) extends EBCode

  case class EBAccessCode(code: String, quantity_available: Int) extends EBCode

  //https://developer.eventbrite.com/docs/order-object/
  case class EBOrder(id: String, first_name: String, email: String, costs: EBCosts, attendees: Seq[EBAttendee]) extends EBObject {
    val ticketCount = attendees.length
    val totalCost = costs.gross.value / 100f
    val totalCostText = if(totalCost > 0) Price(totalCost, GBP).pretty else "free"
  }

  object EBOrder {
    val expansions = Seq("attendees")
  }

  case class EBCosts(gross: EBCost) extends EBObject

  case class EBCost(value: Int) extends EBObject

  case class EBAttendee(quantity: Int, ticket_class_id: String) extends EBObject
}

object EventbriteDeserializer {
  import Eventbrite._

  private def ebResponseReads[T](namespace: String)(implicit reads: Reads[Seq[T]]): Reads[EBResponse[T]] =
    ((JsPath \ "pagination").read[EBPagination](ebPaginationReads) and
      (JsPath \ namespace).read[Seq[T]])(EBResponse[T] _)

  private def convertInstantText(utc: String): Instant =
    ISODateTimeFormat.dateTimeNoMillis.parseDateTime(utc).toInstant

  private def convertDateText(utc: String, timezone: String): DateTime = {
    val timeZone = DateTimeZone.forID(timezone)
    ISODateTimeFormat.dateTimeNoMillis.parseDateTime(utc).withZone(timeZone)
  }

  // Remove any leading/trailing spaces left by the events team
  implicit val readsTrimString = Reads[String] {
    case JsString(s) => JsSuccess(s.trim)
    case _ => JsError(JsPath() -> JsonValidationError("error.expected.jsstring"))
  }

  implicit val instant: Reads[Instant] = JsPath.read[String].map(convertInstantText)

  implicit val readsEbDate: Reads[DateTime] = (
    (JsPath \ "utc").read[String] and
      (JsPath \ "timezone").read[String]
    )(convertDateText _)

  implicit val ebError = Json.reads[EBError]
  implicit val ebLocation = Json.reads[EBAddress]

  implicit val ebVenue = new Reads[EBVenue] {

    private val reader = Json.reads[EBVenue]

    override def reads(json: JsValue) = json match {
      case JsNull => JsSuccess(EBVenue(None, None))
      case _ => reader.reads(json)
    }
  }

  implicit val ebRichText: Reads[EBRichText] = (
    (JsPath \ "text").readNullable[String].map(_.getOrElse("")) and
      (JsPath \ "html").readNullable[String].map(_.getOrElse(""))
    )(EBRichText.apply _)

  implicit val ebPricingReads = Json.reads[EBPricing]
  implicit val ebTicketsReads = Json.reads[EBTicketClass]
  implicit val ebEventReads = Json.reads[EBEvent]
  implicit val ebDescriptionReads = Json.reads[EBDescription]
  implicit val ebDiscountReads = Json.reads[EBDiscount]
  implicit val ebAccessCodeReads = Json.reads[EBAccessCode]

  implicit val ebPaginationReads = Json.reads[EBPagination]
  implicit val ebEventsReads = ebResponseReads[EBEvent]("events")
  implicit val ebDiscountsReads = ebResponseReads[EBDiscount]("discounts")
  implicit val ebAccessCodesReads = ebResponseReads[EBAccessCode]("access_codes")

  implicit val ebCostReads = Json.reads[EBCost]
  implicit val ebCostsReads = Json.reads[EBCosts]
  implicit val ebAttendeeReads = Json.reads[EBAttendee]
  implicit val ebOrderReads = Json.reads[EBOrder]
}
