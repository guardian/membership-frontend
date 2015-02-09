package tracking

import java.util.{List => JList, Map => JMap}

import com.gu.membership.salesforce.MemberId
import com.snowplowanalytics.snowplow.tracker._
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import configuration.Config
import forms.MemberForm.{MarketingChoicesForm, PaidMemberJoinForm, JoinForm}
import model.IdUser
import com.github.t3hnar.bcrypt._
import scala.collection.JavaConversions._
import play.api.Logger

case class SingleEvent (user: EventSubject, eventSource: String) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> eventSource, "from" -> user.toMap)
}

object SingleEvent {
  def apply(memberId: MemberId, user: IdUser, userFormData: JoinForm, eventSource: String): SingleEvent  = {

    val subscriptionPlan = userFormData match {
      case paidMemberJoinForm: PaidMemberJoinForm => if (paidMemberJoinForm.payment.annual) Some("annual") else Some("monthly")
      case _ => None
    }

    val billingPostcode = userFormData match {
      case paidMemberJoinForm: PaidMemberJoinForm => paidMemberJoinForm.billingAddress.map(_.postCode).orElse(Some(paidMemberJoinForm.deliveryAddress.postCode))
      case _ => None
    }

    SingleEvent(
      EventSubject(
        salesforceContactId = memberId.salesforceContactId,
        identityId = user.id,
        deliveryPostcode = userFormData.deliveryAddress.postCode,
        billingPostcode =  billingPostcode,
        tier = userFormData.plan.salesforceTier,
        subscriptionPlan = subscriptionPlan,
        marketingChoices = userFormData.marketingChoices
      
      ), eventSource)
  }
}

trait TrackerData {
  def user: EventSubject
  def eventSource: String
  def toMap: JMap[String, Object]
}

case class EventSubject(salesforceContactId: String,
                        identityId: String,
                        deliveryPostcode: String,
                        billingPostcode: Option[String],
                        tier: String,
                        subscriptionPlan: Option[String],
                        marketingChoices: MarketingChoicesForm
                         ) {
  def toMap: JMap[String, Object] = {

    def bcrypt(string: String) = string+Config.bcryptPepper.bcrypt(Config.bcryptSalt)


    val dataMap = Map(
      "salesforceContactId" -> bcrypt(salesforceContactId),
      "identityId" -> bcrypt(identityId),
      "tier" -> tier,
      "deliveryPostcode" -> deliveryPostcode,
      "marketingChoicesForm" -> EventTracking.setSubMap(Map(
        "gnm" -> marketingChoices.gnm.getOrElse(false),
        "thirdParty" -> marketingChoices.thirdParty.getOrElse(false),
        "membership" -> true
      ))
    ) ++
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