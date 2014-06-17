package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

object Stripe {

  trait StripeObject

  case class Error(`type`: String, message: String, code: Option[String], decline_code: Option[String]) extends Throwable with StripeObject {
    override def getMessage:String = `type` + s" $message "
  }

  case class StripeList[T](total_count: Int, data: Seq[T]) extends StripeObject

  case class Card(`type`: String, last4: String) extends StripeObject

  case class Charge(amount: Int, currency: String, card: Card, description: Option[String])
    extends StripeObject

  case class Customer(id: String, subscriptions: StripeList[Subscription], cards: StripeList[Card]) extends StripeObject

  case class Subscription(id: String, start: Long, current_period_end: Long, plan: Plan) extends StripeObject

  case class Plan(id: String, name: String, amount: Int) extends StripeObject {
    val tier = Tier.withName(id)
  }
}

object StripeDeserializer {
  import Stripe._

  implicit val readsError = Json.reads[Error]
  implicit val readsCard = Json.reads[Card]
  implicit val readsCharge = Json.reads[Charge]
  implicit val readsPlan = Json.reads[Plan]
  implicit val readsSubscription = Json.reads[Subscription]

  implicit def readsList[T](implicit reads: Reads[Seq[T]]): Reads[StripeList[T]] =
    ((JsPath \ "total_count").read[Int] and (JsPath \ "data").read[Seq[T]])(StripeList[T] _)

  implicit val readsCustomer = Json.reads[Customer]
}

object StripeSerializer {
  import Stripe._

  implicit val writesError = Json.writes[Error]
}
