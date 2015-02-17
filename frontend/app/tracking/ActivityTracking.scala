package tracking

import java.util.{List => JList, Map => JMap}

import com.github.nscala_time.time.Imports._
import com.github.t3hnar.bcrypt._
import com.gu.membership.salesforce.Tier
import com.snowplowanalytics.snowplow.tracker.{Tracker, Subject}
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod
import com.snowplowanalytics.snowplow.tracker.core.emitter.{RequestMethod, HttpMethod}
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import com.snowplowanalytics.snowplow.tracker.{Subject, Tracker}
import configuration.Config
import forms.MemberForm.MarketingChoicesForm
import model.Eventbrite.{EBTicketClass, EBEvent}
import model.RichEvent.RichEvent
import play.api.Logger

import scala.collection.JavaConversions._

case class MemberActivity (source: String, member: MemberData) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> source) ++ member.toMap
}

case class EventActivity(source: String, member: Option[MemberData], eventData: EventData) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> source) ++ eventData.toMap ++ member.fold(ActivityTracking.setSubMap(Map.empty))(_.toMap)
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
            tierAmend.effectiveFromDate.map("effectiveDate" -> _)
          }
        }

    ActivityTracking.setSubMap(dataMap)
  }

  def truncatePostcode(postcode: String) = {
    postcode.splitAt(postcode.length-3)._1.trim
  }
}

case class EventData(event: RichEvent) {
  def toMap: JMap[String, Object] = {

    val dataMap = Map(
      "id" -> event.id,
      "startTime" -> event.start,
      "endTime" -> event.end,
      "created" -> event.created,
      "capacity" -> event.capacity,
      "status" -> event.status,
      "isBookable" -> event.isBookable,
      "isFree" -> event.isFree,
      "isPastEvent" -> event.isPastEvent,
      "isSoldOut" -> event.isSoldOut,
      "isSoldThruEventbrite" -> event.isSoldThruEventbrite
    ) ++ Map("ticketClasses" -> event.ticket_classes.map(ticketClassToMap(_))) ++
      Map("tags" -> event.tags.toList)
    event.venue.address.flatMap(a=> a.postal_code).map("postCode" -> _) ++
      event.providerOpt.map("provider" -> _)

    ActivityTracking.setSubMap(dataMap)
  }

  private def ticketClassToMap(ticketClass: EBTicketClass): JMap[String, Object] = {
    val dataMap = Map(
      "id" -> ticketClass.id,
      "name" -> ticketClass.name,
      "free" -> ticketClass.free,
      "quantityTotal" -> ticketClass.quantity_total,
      "quantitySold" -> ticketClass.quantity_sold,
      "saleEnds" -> ticketClass.sales_end
    ) ++
      ticketClass.cost.map("value" -> _.value) ++
      ticketClass.cost.map("discountPrice" -> _.discountPrice) ++
      ticketClass.cost.map("savingPrice" -> _.savingPrice) ++
      ticketClass.cost.map("formattedPrice" -> _.formattedPrice) ++
      ticketClass.hidden.map("hidden" -> _)

    ActivityTracking.setSubMap(dataMap)

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