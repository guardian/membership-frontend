package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

object Stripe {

  trait StripeObject

  case class Error(`type`: String, message: String, code: Option[String], decline_code: Option[String]) extends Throwable with StripeObject {
    override def getMessage:String = `type` + s" $message "
  }

  case class StripeList[T](total_count: Int, data: Seq[T]) extends StripeObject

  case class Card(id: String, `type`: String, last4: String) extends StripeObject

  case class Customer(id: String, cards: StripeList[Card]) extends StripeObject {
    // customers should always have a card
    if (cards.total_count != 1) {
      throw Error("internal", s"Customer $id has ${cards.total_count} cards, should have exactly one", None, None)
    }

    val card = cards.data(0)
  }
}

object StripeDeserializer {
  import Stripe._

  implicit val readsError = Json.reads[Error]
  implicit val readsCard = Json.reads[Card]

  implicit def readsList[T](implicit reads: Reads[Seq[T]]): Reads[StripeList[T]] =
    ((JsPath \ "total_count").read[Int] and (JsPath \ "data").read[Seq[T]])(StripeList[T] _)

  implicit val readsCustomer = Json.reads[Customer]
}

object StripeSerializer {
  import Stripe._

  implicit val writesError = Json.writes[Error]
}
