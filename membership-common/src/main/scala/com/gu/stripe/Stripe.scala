package com.gu.stripe

import com.gu.i18n.Currency
import com.gu.i18n.Currency.GBP
import org.joda.time.{DateTime, Instant, LocalDate}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object Stripe {

  sealed trait StripeObject

  // see https://stripe.com/docs/api#errors
  case class Error(
    `type`: String,
    charge: Option[String] = None,
    message: Option[String] = None,
    code: Option[String] = None,
    decline_code: Option[String] = None,
    param: Option[String] = None
  ) extends Throwable with StripeObject {
    override def getMessage: String = s"message: $message; type: ${`type`}; code: $code; decline_code: $decline_code"
  }

  case class Event[+T <: StripeObject](id: String, `object`: T, liveMode: Boolean) extends StripeObject

  case class StripeList[T](total_count: Int, data: Seq[T]) extends StripeObject

  case class Card(id: String, `type`: String, last4: String, exp_month: Int, exp_year: Int, country: String) extends StripeObject {
    val issuer = `type`.toLowerCase
  }

  case class Subscription(
    id: String,
    created: LocalDate,
    currentPeriodStart: LocalDate,
    currentPeriodEnd: LocalDate,
    cancelledAt: Option[LocalDate],
    cancelAtPeriodEnd: Boolean,
    customer: SubscriptionCustomer,
    plan: SubscriptionPlan,
    status: String,
  ) extends StripeObject {
    // https://stripe.com/docs/api/subscriptions/object#subscription_object-status
    def isCancelled = status == "canceled" || status == "unpaid"

    def isPastDue = status == "past_due"

    def cancellationEffectiveDate = cancelledAt match {
      case Some(_) => cancelledAt
      case None if cancelAtPeriodEnd => Some(currentPeriodEnd)
      case _ => None
    }

    def nextPaymentDate = if (isCancelled) None else Some(currentPeriodEnd)
  }

  case class SubscriptionPlan(id: String, amount: Int, interval: String, currency: Currency)

  case class SubscriptionCustomer(id: String, email: String)

  case class CreateCustomerResponse(id: String) extends StripeObject

  case class Customer(id: String, cards: StripeList[Card]) extends StripeObject {
    // customers should always have EXACTLY ONE card
    if (cards.total_count != 1) {
      throw Error(`type` = "internal", message = Some(s"Customer $id has ${cards.total_count} cards, should have exactly one"))
    }

    val card = cards.data.head
  }

  case class StripePaymentMethodCard(brand: String, last4: String, exp_month: Int, exp_year: Int, country: String) extends StripeObject {
    def asTraditional(paymentMethodId: String) = Card(
      id = paymentMethodId,
      `type` = brand,
      last4,
      exp_month,
      exp_year,
      country,
    )
  }

  case class StripePaymentMethod(id: String, card: StripePaymentMethodCard, customer: String) extends StripeObject

  case class CustomersPaymentMethods(private val data: List[StripePaymentMethod]) extends StripeObject {
    val cardStripeList = StripeList(
      total_count = data.length,
      data = data.map(stripePaymentMethod => stripePaymentMethod.card.asTraditional(stripePaymentMethod.id))
    )
  }

  case class Charge(id: String, amount: Int, balance_transaction: Option[String], created: Int, currency: String, livemode: Boolean,
                    paid: Boolean, refunded: Boolean, receipt_email: String, metadata: Map[String, String], source: Source) extends StripeObject {
  }

  case class Source(country: String) extends StripeObject


  case class BalanceTransaction(id: String, source: String, amount: Int) extends StripeObject


  object Deserializer {
    implicit val sourceFormat = Json.format[Source]
    implicit val balanceTransactionFormat = Json.format[BalanceTransaction]

    implicit val readsCharge = Json.reads[Charge]

    // for stripe objects nested within Event objects
    val readsObject: Reads[StripeObject] = Reads { json =>
      readsCharge.reads(json) //orElse readsCustomer.reads(json) etc
    }

    implicit val readsError = Reads { json =>
      implicit val reader = Json.reads[Error]
      (json \ "error").validate[Error]
    }

    implicit val readsCard = Reads { jsValue =>
      ((jsValue \ "id").validate[String] and
        ((jsValue \ "type").validate[String] orElse (jsValue \ "brand").validate[String]) and
        (jsValue \ "last4").validate[String] and
        (jsValue \ "exp_month").validate[Int] and
        (jsValue \ "exp_year").validate[Int] and
        (jsValue \ "country").validate[String]
        )(Card)
    }

    implicit val readsSource = Json.reads[Source]

    implicit val readsEvent: Reads[Event[StripeObject]] = (
      (__ \ "id").read[String] and
      (__ \ "data" \ "object").read(readsObject) and
      (__ \ "livemode").read[Boolean]
    )(Event.apply[StripeObject] _)


    implicit def readsList[T](implicit reads: Reads[Seq[T]]): Reads[StripeList[T]] =
      ((JsPath \ "total_count").read[Int] and (JsPath \ "data").read[Seq[T]])(StripeList[T] _)

    implicit val readsCreateCustomerResponse: Reads[CreateCustomerResponse] = Json.reads[CreateCustomerResponse]

    implicit val currencyReads: Reads[Currency] = Reads {
      case JsString(str) => Currency.fromString(str.toUpperCase) match {
        case Some(currency) => JsSuccess(currency)
        case None => JsError(s"$str is not a valid currency code")
      }
      case _ => JsError(s"Missing currency code in Stripe response")
    }
    implicit val readsPlan: Reads[SubscriptionPlan] = Reads {
      jsValue => (
        (jsValue \ "id").validate[String] and
        (jsValue \ "amount").validate[Int] and
        (jsValue \ "interval").validate[String] and
        (jsValue \ "currency").validate[Currency]
        )(SubscriptionPlan)
    }

    implicit val readsLocalDate: Reads[LocalDate] = Reads {
      case JsNumber(d) => JsSuccess(Instant.ofEpochSecond(d.toLong).toDateTime.toLocalDate)
      case _ => JsError("Expected an epoch second for the date format")
    }

    implicit val readsSubscription: Reads[Subscription] = Reads {
      jsValue => ((jsValue \ "id").validate[String] and
        (jsValue \ "created").validate[LocalDate] and
        (jsValue \ "current_period_start").validate[LocalDate] and
        (jsValue \ "current_period_end").validate[LocalDate] and
        (jsValue \ "canceled_at").validateOpt[LocalDate] and
        (jsValue \ "cancel_at_period_end").validate[Boolean] and
        (jsValue \ "customer").validate[SubscriptionCustomer] and
        (jsValue \ "plan").validate[SubscriptionPlan] and
        (jsValue \ "status").validate[String]
        )(Subscription)
    }

    implicit val readsStripeCustomer: Reads[SubscriptionCustomer] = Json.reads[SubscriptionCustomer]

    implicit val readsCustomer: Reads[Customer] = Reads { jsValue =>
      ((jsValue \ "id").validate[String] and
        ((jsValue \ "cards").validate[StripeList[Card]] orElse (jsValue \ "sources").validate[StripeList[Card]])
        )(Customer)
    }

    implicit val readsStripePaymentMethodCard: Reads[StripePaymentMethodCard] = Json.reads[StripePaymentMethodCard]

    implicit val readsStripePaymentMethod: Reads[StripePaymentMethod] = Reads { jsValue =>
      ((jsValue \ "id").validate[String] and
        (jsValue \ "card").validate[StripePaymentMethodCard] and
        (jsValue \ "customer").validate[String]
        )(StripePaymentMethod)
    }

    implicit val readsCustomersPaymentMethods: Reads[CustomersPaymentMethods] = Json.reads[CustomersPaymentMethods]

    implicit val readsBalanceTransaction = Json.reads[BalanceTransaction]
  }
}


