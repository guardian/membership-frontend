package model

import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.joda.time.Instant

object Stripe {

  trait StripeObject

  case class Error(`type`: String, message: String, code: Option[String], decline_code: Option[String]) extends Throwable with StripeObject {
    override def getMessage:String = `type` + s" $message "
  }

  case class StripeList[T](total_count: Int, data: Seq[T]) extends StripeObject

  case class Card(`type`: String, last4: String) extends StripeObject

  case class Charge(amount: Int, currency: String, card: Card, description: Option[String])
    extends StripeObject

  case class Customer(id: String, subscriptions: StripeList[Subscription], cards: StripeList[Card]) extends StripeObject {
    // We currently only support one subscription/card
    val subscription = subscriptions.data.headOption
    val card = cards.data.headOption
  }

  case class Subscription(
    id: String,
    start: Instant,
    current_period_start: Instant,
    current_period_end: Instant,
    canceled_at: Option[Instant],
    customer: String,
    plan: Plan) extends StripeObject

  case class Plan(id: String, name: String, amount: Int, interval: String) extends StripeObject {
    val tier = Tier.withName(id.replace(Plan.ANNUAL_SUFFIX, ""))
  }

  object Plan {
    val ANNUAL_SUFFIX = "Annual"
  }

  case class EventData(`object`: JsObject) extends StripeObject
  case class Event(id: String, `type`: String, data: EventData) extends StripeObject {
    def extract[T](implicit reads: Reads[T]) = data.`object`.as[T]
  }
}

object StripeDeserializer {
  import Stripe._

  implicit val readsInstant = JsPath.read[Long].map(l => new Instant(l * 1000))

  implicit val readsError = Json.reads[Error]
  implicit val readsCard = Json.reads[Card]
  implicit val readsCharge = Json.reads[Charge]
  implicit val readsPlan = Json.reads[Plan]
  implicit val readsSubscription = Json.reads[Subscription]

  implicit def readsList[T](implicit reads: Reads[Seq[T]]): Reads[StripeList[T]] =
    ((JsPath \ "total_count").read[Int] and (JsPath \ "data").read[Seq[T]])(StripeList[T] _)

  implicit val readsCustomer = Json.reads[Customer]

  implicit val readsEventData = Json.reads[EventData]
  implicit val readsEvent = Json.reads[Event]
}

object StripeSerializer {
  import Stripe._

  implicit val writesError = Json.writes[Error]
}
