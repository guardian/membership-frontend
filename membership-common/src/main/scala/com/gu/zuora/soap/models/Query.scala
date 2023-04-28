package com.gu.zuora.soap.models

import com.gu.i18n.{Country, Currency}
import com.gu.memsub.Subscription.Feature.{Code, Id}
import com.gu.zuora.api.PaymentGateway
import org.joda.time.LocalDate

trait Query

trait Identifiable { self: Query =>
  def id: String
  def objectName: String = self.getClass.getSimpleName
}

object Queries {

  object PaymentMethod {
    val CreditCard = "CreditCard"
    val CreditCardReferenceTransaction = "CreditCardReferenceTransaction"
    val BankTransfer = "BankTransfer"
    val PayPal = "PayPal"
  }

  case class PaymentMethod(id: String,
                           mandateId: Option[String],
                           tokenId: Option[String],
                           secondTokenId: Option[String],
                           payPalEmail: Option[String],
                           bankTransferType: Option[String],
                           bankTransferAccountName: Option[String],
                           bankTransferAccountNumberMask: Option[String],
                           bankCode: Option[String],
                           `type`: String,
                           creditCardNumber: Option[String],
                           creditCardExpirationMonth: Option[String],
                           creditCardExpirationYear: Option[String],
                           creditCardType: Option[String],
                           numConsecutiveFailures: Option[Int],
                           paymentMethodStatus: Option[String]
                          ) extends Query with Identifiable

  case class ProductRatePlan(id: String,
                             name: String,
                             productId: String,
                             effectiveStartDate: LocalDate,
                             effectiveEndDate: LocalDate) extends Query with Identifiable

  case class ProductRatePlanCharge(id: String,
                                   name: String,
                                   productRatePlanId: String,
                                   billingPeriod: String) extends Query with Identifiable

  case class ProductRatePlanChargeTier(currency: String,
                                       price: Float,
                                       productRatePlanChargeId: String) extends Query

  case class Product(id: String, name: String) extends Query with Identifiable

  case class Account(id: String,
                     billToId: String,
                     soldToId: String,
                     billCycleDay: Int,
                     creditBalance: Float,
                     currency: Option[Currency],
                     defaultPaymentMethodId: Option[String],
                     sfContactId: Option[String],
                     paymentGateway: Option[PaymentGateway]) extends Query with Identifiable

  case class Contact(
    id: String,
    firstName: String,
    lastName: String,
    postalCode: Option[String],
    country: Option[Country],
    email: Option[String]
  ) extends Query with Identifiable

  case class RatePlan(id: String,
                      name: String,
                      productRatePlanId: String) extends Query with Identifiable

  case class RatePlanCharge(id: String,
                            chargedThroughDate: Option[LocalDate],
                            effectiveStartDate: LocalDate,
                            billingPeriod: Option[String],
                            chargeModel: Option[String],
                            chargeType: Option[String],
                            price: Float) extends Query with Identifiable

  case class Subscription(id: String,
                          name: String,
                          accountId: String,
                          version: Int,
                          termStartDate: LocalDate,
                          termEndDate: LocalDate,
                          contractAcceptanceDate: LocalDate,
                          activationDate: Option[LocalDate]) extends Query with Identifiable


  case class Amendment(id: String,
                       amendType: String,
                       contractEffectiveDate: LocalDate,
                       subscriptionId: String) extends Query with Identifiable

  case class InvoiceItem(id: String,
                         price: Float,
                         serviceStartDate: LocalDate,
                         serviceEndDate: LocalDate,
                         chargeNumber: String,
                         productName: String,
                         subscriptionId: String) extends Query with Identifiable {
    val nextPaymentDate = serviceEndDate.plusDays(1)
    /** @deprecated use com.gu.memsub.Subscription with Paid */
    val annual = nextPaymentDate == serviceStartDate.plusYears(1)
  }

  case class PreviewInvoiceItem(price: Float,
                                serviceStartDate: LocalDate,
                                serviceEndDate: LocalDate,
                                productId: String,
                                productRatePlanChargeId: String,
                                chargeName: String,
                                unitPrice: Float) extends Query{
    val renewalDate = serviceEndDate.plusDays(1)
  }


  case class Usage(description: String) extends Query

  case class Feature(id: Id, code: Code) extends Query
}