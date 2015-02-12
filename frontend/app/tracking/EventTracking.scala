package tracking

import java.util.{List => JList, Map => JMap}

import com.github.nscala_time.time.Imports._
import com.github.t3hnar.bcrypt._
import com.gu.membership.salesforce.Tier
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import com.snowplowanalytics.snowplow.tracker.{Subject, Tracker}
import configuration.Config
import forms.MemberForm.MarketingChoicesForm
import play.api.Logger

import scala.collection.JavaConversions._

case class SingleEvent (eventSource: String, user: EventSubject) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> eventSource) ++ user.toMap
}


trait TrackerData {
  def user: EventSubject
  def eventSource: String
  def toMap: JMap[String, Object]
}

sealed trait TierAmendment {
  val tierFrom: Tier
  val tierTo: Tier
  val effectiveFromDate: Option[DateTime]
}

case class UpgradeAmendment(tierFrom: Tier, tierTo: Tier, effectiveFromDate: Option[DateTime] = Some(DateTime.now)) extends TierAmendment
case class DowngradeAmendment(tierFrom: Tier, tierTo: Tier = Tier.Friend, effectiveFromDate: Option[DateTime] = None) extends TierAmendment

case class EventSubject(salesforceContactId: String,
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
          "marketingChoicesForm" -> EventTracking.setSubMap {
            Map(
              "gnm" -> mc.gnm.getOrElse(false),
              "thirdParty" -> mc.thirdParty.getOrElse(false),
              "membership" -> true
            )
          }
        } ++
        tierAmendment.map { tierAmend =>
          "amendTier" -> EventTracking.setSubMap {
            Map(
              "from" -> tierAmend.tierFrom.name,
              "to" -> tierAmend.tierTo.name
            ) ++
            tierAmend.effectiveFromDate.map("effectiveDate" -> _)
          }
        }

    EventTracking.setSubMap(dataMap)
  }

  def truncatePostcode(postcode: String) = {
    postcode.splitAt(postcode.length-3)._1.trim
  }
}

trait EventTracking {

  def trackEvent(data: TrackerData) {
    try {
      val tracker = getTracker
      val dataMap = data.toMap
      tracker.trackUnstructuredEvent(dataMap)
    } catch {
      case error: Throwable =>
      Logger.error(s"Event tracking error: ${error.getMessage}")
    }
  }

  private def getTracker: Tracker = {
    val emitter = new Emitter(EventTracking.url, HttpMethod.GET)
    val subject = new Subject
    new Tracker(emitter, subject, "membership", "membership-frontend")
  }
}

object EventTracking {
  val url = Config.trackerUrl

  def setSubMap(in:Map[String, Any]): JMap[String, Object] =
    mapAsJavaMap(in).asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]
}