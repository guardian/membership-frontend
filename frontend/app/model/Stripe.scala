package model

object Stripe {

  trait StripeObject

  case class Error(`type`: String, message: String) extends Throwable with StripeObject

  case class Card(`type`: String, last4: String) extends StripeObject

  case class CardList(data: List[Card]) extends StripeObject

  case class Charge(amount: Int, currency: String, card: Card, description: String)
    extends StripeObject

  case class Customer(id: String, subscriptions: SubscriptionList, cards: CardList) extends StripeObject

  case class Subscription(id: String, start: Long, current_period_end: Long, plan: Plan) extends StripeObject

  case class SubscriptionList(data: List[Subscription]) extends StripeObject

  case class Plan(id: String, name: String, amount: Int) extends StripeObject
}
