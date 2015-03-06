package tracking

import java.util.{List => JList, Map => JMap}

import com.github.t3hnar.bcrypt._
import com.gu.membership.salesforce.Tier
import com.snowplowanalytics.snowplow.tracker.core.emitter.{HttpMethod, RequestMethod}
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import com.snowplowanalytics.snowplow.tracker.{Subject, Tracker}
import configuration.Config
import forms.MemberForm.MarketingChoicesForm
import model.Eventbrite.{EBOrder, EBTicketClass}
import model.RichEvent.{DiscoverEvent, MasterclassEvent, GuLiveEvent, RichEvent}
import play.api.Logger
import org.joda.time._

import scala.collection.JavaConversions._

case class MemberActivity (source: String, member: MemberData) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> source) ++ member.toMap
}

case class EventActivity(source: String, member: Option[MemberData],
                         eventData: EventData, order: Option[OrderData] = None) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> source) ++
      eventData.toMap ++
      member.fold(ActivityTracking.setSubMap(Map.empty))(_.toMap) ++
      order.fold(ActivityTracking.setSubMap(Map.empty))(_.toMap)
}

trait TrackerData {
  def source: String
  def toMap: JMap[String, Object]
}

sealed trait TierAmendment {
  val tierFrom: Tier
  val tierTo: Tier
  val effectiveFromDate: Option[DateTime]
}

case class UpgradeAmendment(tierFrom: Tier, tierTo: Tier, effectiveFromDate: Option[DateTime] = Some(DateTime.now)) extends TierAmendment
case class DowngradeAmendment(tierFrom: Tier, tierTo: Tier = Tier.Friend, effectiveFromDate: Option[DateTime] = None) extends TierAmendment

case class MemberData(salesforceContactId: String,
                        identityId: String,
                        tier: String,
                        tierAmendment: Option[TierAmendment] = None,
                        deliveryPostcode: Option[String] = None,
                        billingPostcode: Option[String] = None,
                        subscriptionPaymentAnnual: Option[Boolean] = None,
                        marketingChoices: Option[MarketingChoicesForm] = None) {

  val subscriptionPlan = subscriptionPaymentAnnual match {
    case Some(true) =>  Some("annual")
    case Some(false) => Some("monthly")
    case None => None
  }

  def toMap: JMap[String, Object] = {

    def bcrypt(string: String) = (string+Config.bcryptPepper).bcrypt(Config.bcryptSalt)

    val dataMap =
      Map(
        "salesforceContactId" -> bcrypt(salesforceContactId),
        "identityId" -> bcrypt(identityId),
        "tier" -> tier
      ) ++
        deliveryPostcode.map("deliveryPostcode" -> truncatePostcode(_)) ++
        billingPostcode.map("billingPostcode" -> truncatePostcode(_)) ++
        subscriptionPlan.map("subscriptionPlan" -> _) ++
        marketingChoices.map { mc =>
          "marketingChoicesForm" -> ActivityTracking.setSubMap {
            Map(
              "gnm" -> mc.gnm.getOrElse(false),
              "thirdParty" -> mc.thirdParty.getOrElse(false),
              "membership" -> true
            )
          }
        } ++
        tierAmendment.map { tierAmend =>
          "amendTier" -> ActivityTracking.setSubMap {
            Map(
              "from" -> tierAmend.tierFrom.name,
              "to" -> tierAmend.tierTo.name
            ) ++
            tierAmend.effectiveFromDate.map("startDate" -> _.getMillis)
          }
        }

    val memberMap = Map("member" -> ActivityTracking.setSubMap(dataMap))

    ActivityTracking.setSubMap(memberMap)
  }

  def truncatePostcode(postcode: String) = {
    postcode.splitAt(postcode.length-3)._1.trim
  }
}

case class EventData(event: RichEvent) {

  val group = event match {
    case _: GuLiveEvent => "Guardian Live"
    case _: DiscoverEvent => "Discover"
    case _: MasterclassEvent => "Masterclass"

  }
  def toMap: JMap[String, Object] = {

    val dataMap = Map(
      "id" -> event.id,
      "startTime" -> event.start.getMillis,
      "endTime" -> event.end.getMillis,
      "created" -> event.created.getMillis,
      "capacity" -> event.capacity,
      "status" -> event.status,
      "isBookable" -> event.isBookable,
      "isPastEvent" -> event.isPastEvent,
      "isSoldOut" -> event.isSoldOut,
      "group" -> group
    ) ++
      Map("tags" -> seqAsJavaList(event.tags)) ++
      event.internalTicketing.map("isFree" -> _.isFree) ++
      event.internalTicketing.map("ticketsSold" -> _.ticketsSold) ++
      event.internalTicketing.map("saleEnds" -> _.salesEnd.getMillis) ++
      event.internalTicketing.map("isCurrentlyAvailableToPaidMembersOnly" -> _.isCurrentlyAvailableToPaidMembersOnly) ++
      event.internalTicketing.flatMap(_.generalReleaseTicketOpt).map("generalReleaseTicketOpt" -> ticketClassToMap(_)) ++
      event.internalTicketing.flatMap(_.memberBenefitTicketOpt).map("memberBenefitTicketOpt" -> ticketClassToMap(_)) ++
      event.venue.address.flatMap(a=> a.postal_code).map("postCode" -> _) ++
      event.providerOpt.map("provider" -> _)

    val eventMap = Map("event" -> ActivityTracking.setSubMap(dataMap))

    ActivityTracking.setSubMap(eventMap)
  }

  private def ticketClassToMap(ticketClass: EBTicketClass): JMap[String, Object] = {
    val dataMap = Map(
      "id" -> ticketClass.id,
      "name" -> ticketClass.name,
      "free" -> ticketClass.free,
      "quantityTotal" -> ticketClass.quantity_total,
      "quantitySold" -> ticketClass.quantity_sold,
      "saleEnds" -> ticketClass.sales_end.getMillis,
      "durationBeforeSaleEnds" -> (ticketClass.sales_end.getMillis - DateTime.now.getMillis)
    ) ++
      ticketClass.sales_start.map("salesStart" -> _.getMillis) ++
      ticketClass.sales_start.map(s => "durationAfterSaleStart" -> ( DateTime.now.getMillis - s.getMillis)) ++
      ticketClass.cost.map("value" -> _.value) ++
      ticketClass.cost.map("formattedPrice" -> _.formattedPrice.replace("£", "")) ++
      ticketClass.hidden.map("hidden" -> _)

    ActivityTracking.setSubMap(dataMap)
  }
}

case class OrderData(order: EBOrder) {

  def bcrypt(string: String) = (string+Config.bcryptPepper).bcrypt(Config.bcryptSalt)
  def toMap: JMap[String, Object] = {

    val dataMap = Map(
      "ticketCount" -> order.ticketCount,
      "totalCost" -> order.totalCost,
      "orderId" -> bcrypt(order.id)
    )
    val orderMap = Map("order" -> ActivityTracking.setSubMap(dataMap))
    ActivityTracking.setSubMap(orderMap)

  }
}

trait ActivityTracking {

  def track(data: TrackerData) {
    try {
      val tracker = getTracker
      val dataMap = data.toMap
      tracker.trackUnstructuredEvent(dataMap)
    } catch {
      case error: Throwable =>
      Logger.error(s"Activity tracking error: ${error.getMessage}")
    }
  }

  private def getTracker: Tracker = {
    val emitter = new Emitter(ActivityTracking.url, HttpMethod.GET)
    emitter.setRequestMethod(RequestMethod.Asynchronous)
    val subject = new Subject
    new Tracker(emitter, subject, "membership", "membership-frontend")
  }
}

object ActivityTracking {
  val url = Config.trackerUrl

  def setSubMap(in:Map[String, Any]): JMap[String, Object] =
    mapAsJavaMap(in).asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]
}