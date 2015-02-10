package tracking

import java.util.{List => JList, Map => JMap}

import com.gu.membership.salesforce.{Member, Tier, MemberId}
import com.snowplowanalytics.snowplow.tracker._
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import configuration.Config
import forms.MemberForm.{PaidMemberChangeForm, MarketingChoicesForm, PaidMemberJoinForm, JoinForm}
import model.{IdMinimalUser, IdUser}
import com.github.t3hnar.bcrypt._
import scala.collection.JavaConversions._
import play.api.Logger

case class SingleEvent (eventSource: String, user: EventSubject) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> eventSource) ++ user.toMap
}

object SingleEvent {

  def apply(eventSource: String,
            member: Member,
            newTier: Option[String] = None,
            deliveryPostcode: Option[String] = None,
            billingPostcode: Option[String] = None,
            subscriptionPaymentAnnual: Option[Boolean] = None,
            marketingChoices: Option[MarketingChoicesForm] = None): SingleEvent = {

    SingleEvent(
      eventSource,
      EventSubject(
        salesforceContactId = member.salesforceContactId,
        identityId = member.identityId,
        tier = member.tier.name,
        newTier = newTier,
        deliveryPostcode = deliveryPostcode,
        billingPostcode = billingPostcode.orElse(deliveryPostcode),
        subscriptionPaymentAnnual = subscriptionPaymentAnnual,
        marketingChoices = None
      ))
  }
}

trait TrackerData {
  def user: EventSubject
  def eventSource: String
  def toMap: JMap[String, Object]
}

case class EventSubject(salesforceContactId: String,
                        identityId: String,
                        tier: String,
                        newTier: Option[String],
                        deliveryPostcode: Option[String],
                        billingPostcode: Option[String],
                        subscriptionPaymentAnnual: Option[Boolean],
                        marketingChoices: Option[MarketingChoicesForm]) {

  val subscriptionPlan = subscriptionPaymentAnnual match {
    case Some(true) =>  Some("annual")
    case Some(false) => Some("monthly")
    case None => None
  }

  def toMap: JMap[String, Object] = {

    def bcrypt(string: String) = string+Config.bcryptPepper.bcrypt(Config.bcryptSalt)


    val dataMap = Map(
      "salesforceContactId" -> bcrypt(salesforceContactId),
      "identityId" -> bcrypt(identityId),
      "tier" -> tier
    ) ++
      marketingChoices.map { mc =>
        "marketingChoicesForm" -> EventTracking.setSubMap(Map(
          "gnm" -> mc.gnm.getOrElse(false),
          "thirdParty" -> mc.thirdParty.getOrElse(false),
          "membership" -> true
        ))
      } ++
      newTier.map("newTier" -> _) ++
      deliveryPostcode.map("deliveryPostcode" -> _) ++
      billingPostcode.map("billingPostcode" -> _) ++
      subscriptionPlan.map("subscriptionPlan" -> _)


    EventTracking.setSubMap(dataMap)
  }
}

trait EventTracking {

  def trackEvent(data: TrackerData) {
    try {
      val tracker = getTracker(data.user.salesforceContactId)
      val dataMap = data.toMap
      tracker.trackUnstructuredEvent(dataMap)
    } catch {
      case error: Throwable =>
      Logger.error(s"Event tracking error: ${error.getMessage}")
    }
  }

  private def getTracker(userId: String): Tracker = {
    val emitter = new Emitter(EventTracking.url, HttpMethod.GET)
    val subject = new Subject
    subject.setUserId(userId)
    new Tracker(emitter, subject, "membership", "membership-frontend")
  }
}

object EventTracking {
  val url = Config.trackerUrl

  def setSubMap(in:Map[String, Any]): JMap[String, Object] = {
    mapAsJavaMap(in).asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]
  }
}