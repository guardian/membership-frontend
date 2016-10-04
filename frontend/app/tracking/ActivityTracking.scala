package tracking
import java.util.{Map => JMap}
import com.github.t3hnar.bcrypt._
import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.BillingPeriod
import com.gu.salesforce._
import com.snowplowanalytics.snowplow.tracker.core.emitter.{HttpMethod, RequestMethod}
import com.snowplowanalytics.snowplow.tracker.emitter.Emitter
import com.snowplowanalytics.snowplow.tracker.{Subject, Tracker}
import configuration.Config
import controllers.Testing
import forms.MemberForm.{AddressDetails, JoinForm, MarketingChoicesForm, PaidMemberJoinForm}
import model.Eventbrite.{EBOrder, EBTicketClass}
import model.RichEvent.{GuLiveEvent, LocalEvent, MasterclassEvent, RichEvent}
import model.GenericSFContact
import org.joda.time._
import play.api.Logger
import play.api.mvc.RequestHeader
import services.EventbriteService
import utils.CampaignCode
import utils.TestUsers.isTestUser
import com.gu.memsub.subsv2._
import views.support.MembershipCompat._
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

case class UpgradeAmendment(tierFrom: Tier, tierTo: PaidTier, effectiveFromDate: Option[DateTime] = Some(DateTime.now)) extends TierAmendment
case class DowngradeAmendment(tierFrom: Tier, effectiveFromDate: Option[DateTime] = None) extends TierAmendment {
  val tierTo = Tier.friend
}

case class MemberData(salesforceContactId: String,
                      identityId: String,
                      tier: Tier,
                      tierAmendment: Option[TierAmendment] = None,
                      deliveryPostcode: Option[String] = None,
                      billingPostcode: Option[String] = None,
                      subscriptionPaymentAnnual: Option[Boolean] = None,
                      marketingChoices: Option[MarketingChoicesForm] = None,
                      city: Option[String] = None,
                      country: Option[String] = None,
                      campaignCode: Option[CampaignCode] = None) {

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
        "tier" -> tier.name
      ) ++
        deliveryPostcode.map("deliveryPostcode" -> truncatePostcode(_)) ++
        billingPostcode.map("billingPostcode" -> truncatePostcode(_)) ++
        subscriptionPlan.map("subscriptionPlan" -> _) ++
        city.map("city" -> _) ++
        country.map("country" -> _) ++
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
        } ++ campaignCode.map { code =>
        "campaignCode" -> code.get
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
    case _: LocalEvent => "Local"
    case _: MasterclassEvent => "Masterclass"

  }
  def toMap: JMap[String, Object] = {

    val dataMap = Map(
      "id" -> event.id,
      "name" -> event.name.text,
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

  def trackAnon(data: TrackerData)(implicit request: RequestHeader) {
    val analyticsOff = request.cookies.get(Testing.AnalyticsCookieName).isDefined

    if (!analyticsOff) executeTracking(data)
  }

  def track(data: TrackerData, user: IdMinimalUser) {
    if (!isTestUser(user)) executeTracking(data)
  }

  def track(data: TrackerData, member: Contact) {
    if (!isTestUser(member)) executeTracking(data)
  }

  def trackRegistration(formData: JoinForm, tier: Tier, member: ContactId, user: IdMinimalUser, campaignCode: Option[CampaignCode]) {
    val subscriptionPaymentAnnual = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm => Some(paidMemberJoinForm.payment.billingPeriod.annual)
      case _ => None
    }

    val billingPostcode = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm =>
        paidMemberJoinForm.billingAddress.map(_.postCode).orElse(Some(formData.deliveryAddress.postCode))
      case _ => None
    }

    val trackingInfo =
      MemberData(
        salesforceContactId = member.salesforceContactId,
        identityId = user.id,
        tier = tier,
        tierAmendment = None,
        deliveryPostcode = Some(formData.deliveryAddress.postCode),
        billingPostcode = billingPostcode,
        subscriptionPaymentAnnual = subscriptionPaymentAnnual,
        marketingChoices = Some(formData.marketingChoices),
        city = Some(formData.deliveryAddress.town),
        country = Some(formData.deliveryAddress.country.fold(formData.deliveryAddress.countryName)(_.name)),
        campaignCode = campaignCode
      )

    track(MemberActivity("membershipRegistration", trackingInfo), user)
  }

  def trackRegistrationViaEvent(
      contactId: ContactId,
      user: IdMinimalUser,
      fromEventId: Option[String],
      campaignCode: Option[CampaignCode],
      tier: Tier) {

    import EventbriteService._

    fromEventId.flatMap(EventbriteService.getBookableEvent).foreach { event =>
      event.service.wsMetrics.put(s"join-${tier.name}-event", 1)

      val memberData = MemberData(
        salesforceContactId = contactId.salesforceContactId,
        identityId = user.id,
        tier = tier,
        campaignCode = campaignCode)

      track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)), user)
    }
  }

  def trackUpgrade(member: GenericSFContact, subscription: Subscription[SubscriptionPlan.Member],
                   newRatePlan: CatalogPlan.PaidMember[BillingPeriod],
                   addressDetails: Option[AddressDetails],
                   campaignCode: Option[CampaignCode]): Unit = {

    track(
      MemberActivity(source = "membershipUpgrade",
        MemberData(
          salesforceContactId = member.salesforceContactId,
          identityId = member.identityId,
          tier = subscription.plan.tier,
          tierAmendment = Some(UpgradeAmendment(subscription.plan.tier, newRatePlan.tier)),
          deliveryPostcode = addressDetails.map(_.deliveryAddress.postCode),
          billingPostcode = addressDetails.flatMap(f => f.billingAddress.map(_.postCode)).orElse(addressDetails.map(_.deliveryAddress.postCode)),
          subscriptionPaymentAnnual = Some(newRatePlan.charges.billingPeriod.annual),
          marketingChoices = None,
          city = addressDetails.map(_.deliveryAddress.town),
          country = addressDetails.map(addressDetails => addressDetails.deliveryAddress.country.fold(addressDetails.deliveryAddress.countryName)(_.name)),
          campaignCode = campaignCode
        )),
      member)
  }

  private def executeTracking(data: TrackerData) {
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
