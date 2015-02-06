package tracking

import java.util.{List => JList, Map => JMap}

import com.gu.membership.salesforce.MemberId
import com.snowplowanalytics.snowplow.tracker._
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import configuration.Config
import forms.MemberForm.JoinForm
import model.IdUser
import com.github.t3hnar.bcrypt._
import scala.collection.JavaConversions._



case class SingleEvent (user: EventSubject, eventSource: String) extends TrackerData {
  def toMap: JMap[String, Object] =
    Map("eventSource" -> eventSource, "from" -> user.toMap)
}

object SingleEvent {
  def apply(memberId: MemberId, user: IdUser, userFormData: JoinForm, eventSource: String): SingleEvent  = {
    SingleEvent(EventSubject(memberId.salesforceContactId, user.id, userFormData.deliveryAddress.postCode), eventSource)
  }
}

trait TrackerData {
  def user: EventSubject
  def eventSource: String
  def toMap: JMap[String, Object]
}

case class EventSubject(salesforceContactId: String,
                        identityId: String,
                        postcode: String) {
  def toMap: JMap[String, Object] = {

    def bcrypt(string: String) = string+Config.bcryptPepper.bcrypt(Config.bcryptSalt)

    EventTracking.setSubMap(
      Map(
        "salesforceContactId" -> bcrypt(salesforceContactId),
        "identityId" -> bcrypt(identityId),
        "postcode" -> postcode
      )
    )
  }
}

trait EventTracking {

  def trackEvent(data: TrackerData) {
    try {
      val tracker = getTracker(data.user.salesforceContactId)
      val dataMap = data.toMap
      tracker.trackUnstructuredEvent(dataMap)
    } catch {
      case _: Throwable =>
      println("ERROR")
      //TODO log, push an aws metric to alert on?
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