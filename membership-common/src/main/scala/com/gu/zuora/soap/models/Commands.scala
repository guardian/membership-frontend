package com.gu.zuora.soap.models

import com.gu.i18n.{Country, Currency}
import com.gu.memsub.Subscription.AccountId
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.subsv2.ReaderType
import com.gu.memsub.{Address, FullName, NormalisedTelephoneNumber, SupplierCode}
import com.gu.salesforce.ContactId
import com.gu.zuora.api.{InvoiceTemplate, PaymentGateway}
import com.gu.zuora.soap.models.Queries.Contact
import org.joda.time.LocalDate
import scalaz.NonEmptyList

object Commands {

  case class Account(contactId: ContactId, identityId: String, currency: Currency, autopay: Boolean, paymentGateway: PaymentGateway, invoiceTemplate: Option[InvoiceTemplate] = None)

  sealed trait PaymentMethod
  case class CreditCardReferenceTransaction(cardId: String, customerId: String, last4: String, cardCountry: Option[Country], expirationMonth: Int, expirationYear: Int, cardType: String) extends PaymentMethod
  case class BankTransfer(accountHolderName: String, accountNumber: String, sortCode: String, firstName: String, lastName: String, countryCode: String) extends PaymentMethod
  case class PayPalReferenceTransaction(baId: String, email: String) extends PaymentMethod

  sealed trait PeriodType
  case object Months extends PeriodType
  case object Quarters extends PeriodType
  case object Years extends PeriodType
  case object SingleYear extends PeriodType

  sealed trait EndDateCondition
  case object SubscriptionEnd extends EndDateCondition
  case class FixedPeriod(upToPeriods: Short, upToPeriodsType: PeriodType) extends EndDateCondition

  case class ChargeOverride(productRatePlanChargeId: String,
                            discountPercentage: Option[Double] = None,
                            triggerDate: Option[LocalDate] = None,
                            endDateCondition: Option[EndDateCondition] = None,
                            billingPeriod: Option[PeriodType] = None,
                            price: Option[BigDecimal] = None)
  
  case class RatePlan(productRatePlanId: String, chargeOverride: Option[ChargeOverride], featureIds: Seq[String] = Nil)
  case class UpdatePromoCode(subscriptionId: String, promoCode: String)

  case class Subscribe(
    account: Account,
    paymentMethod: Option[PaymentMethod] = None,
    ratePlans: NonEmptyList[RatePlan],
    name: FullName,
    address: Address,
    email: String,
    promoCode: Option[PromoCode] = None,
    supplierCode: Option[SupplierCode] = None,
    contractEffective: LocalDate,  // Date term starts and subscription is 'acquired'.
    contractAcceptance: LocalDate, // Date customer starts paying. Usually the same day as first fulfilment.
    soldToContact: Option[SoldToContact] = None,
    ipCountry: Option[Country] = None,
    phone: Option[NormalisedTelephoneNumber] = None,
    readerType: ReaderType
  )

  case class Contribute(account: Account,
                       paymentMethod: Option[PaymentMethod] = None,
                       ratePlans: NonEmptyList[RatePlan],
                       name: FullName,
                       email: String,
                       contractEffective: LocalDate,
                       contractAcceptance: LocalDate,
                       country: String)

  case class Amend(
    subscriptionId: String,
    plansToRemove: Seq[String],
    newRatePlans: NonEmptyList[RatePlan],
    promoCode: Option[PromoCode] = None,
    previewMode: Boolean = false
  )

  case class Renew(
    subscriptionId: String,
    currentTermStartDate: LocalDate,
    currentTermEndDate: LocalDate,
    planToRemove: String,
    newRatePlans: NonEmptyList[RatePlan],
    newTermStartDate: LocalDate,
    promoCode: Option[PromoCode] = None,
    autoRenew : Boolean,
    fastForwardTermStartDate: Boolean
  )

  case class CreatePaymentMethod(accountId: AccountId, paymentMethod: PaymentMethod, paymentGateway: PaymentGateway, billtoContact: Contact, invoiceTemplateOverride: Option[InvoiceTemplate])

  case class SoldToContact(
    name: FullName,
    address: Address,
    deliveryInstructions: Option[String],
    email: Option[String],
    phone: Option[NormalisedTelephoneNumber]
  )

}