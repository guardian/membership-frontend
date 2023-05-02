package com.gu.zuora.soap

import com.gu.i18n.Currency
import com.gu.memsub.Subscription.Feature.{Code, Id}
import com.gu.zuora.ZuoraLookup
import com.gu.zuora.api.PaymentGateway
import com.gu.zuora.soap.models.Queries._
import com.gu.zuora.soap.models.Results._
import com.gu.zuora.soap.models.errors._
import org.joda.time.{DateTime, LocalDate}

object Readers {
  implicit val featureReader = readers.Query("Feature", Seq("Id", "FeatureCode")) { result =>
    Feature(Id(result("Id")), Code(result("FeatureCode")))
  }

  implicit val amendmentReader = readers.Query("Amendment", Seq("Id", "Type", "ContractEffectiveDate", "SubscriptionId")) { result =>
    Amendment(
      result("Id"),
      result("Type"),
      new LocalDate(result("ContractEffectiveDate")),
      result("SubscriptionId"))
  }

  implicit val invoiceItemReader = readers.Query("InvoiceItem",
    Seq("Id", "ChargeAmount", "TaxAmount", "ServiceStartDate", "ServiceEndDate", "ChargeNumber", "ProductName", "SubscriptionId")) { result =>

    InvoiceItem(
      result("Id"),
      result("ChargeAmount").toFloat + result("TaxAmount").toFloat,
      new LocalDate(result("ServiceStartDate")),
      new LocalDate(result("ServiceEndDate")),
      result("ChargeNumber"),
      result("ProductName"),
      result("SubscriptionId"))
  }

  implicit val authenticationReader = readers.Reader("loginResponse") { result =>
    Right(Authentication((result \ "Session").text, (result \ "ServerUrl").text))
  }
  
  implicit val createResultReader = readers.Result("createResponse") { result =>
    CreateResult((result \ "Id").text)
  }

  implicit val subscribeResultReader = readers.Result("subscribeResponse") { result =>
    SubscribeResult(
      subscriptionId = (result \ "SubscriptionId").text,
      subscriptionName = (result \ "SubscriptionNumber").text,
      accountId = (result \ "AccountId").text
    )
  }

  implicit val amendResultReader = readers.Result.multi("amendResponse") { result =>
    val invoiceItems: Seq[PreviewInvoiceItem] = (result \ "InvoiceDatas" \ "InvoiceItem").map { node =>
      PreviewInvoiceItem(
        (node \ "ChargeAmount").text.toFloat + (node \ "TaxAmount").text.toFloat,
        new LocalDate((node \ "ServiceStartDate").text),
        new LocalDate((node \ "ServiceEndDate").text),
        (node \ "ProductId").text,
        (node \ "ProductRatePlanChargeId").text,
        (node \ "ChargeName").text,
        (node \ "UnitPrice").text.toFloat
      )
    }
    AmendResult((result \ "AmendmentIds").map(_.text), invoiceItems)
  }

  implicit val queryResultReader = readers.Reader("queryResponse") { result =>
    if ((result \ "done").text == "true") {
      val records =
      // Zuora still returns a records node even if there were no results
        if ((result \ "size").text.toInt == 0) {
          Nil
        } else {
          (result \ "records").map { record =>
            record.child.map { node => (node.label, node.text) }.toMap
          }
        }

      Right(QueryResult(records))
    } else {
      Left(QueryError("The query was not complete (we don't support iterating query results)"))
    }
  }

  implicit val updateResultReader = readers.Result("updateResponse") { result =>
    val id = (result \ "Id").text
    UpdateResult(id)
  }

  implicit val paymentMethodReader = readers.Query("PaymentMethod", Seq(
    "Id",
    "Type",
    "NumConsecutiveFailures",
    "PaymentMethodStatus",
    "MandateID",
    "TokenId",
    "SecondTokenId",
    "PaypalEmail",
    "BankTransferType",
    "BankTransferAccountName",
    "BankTransferAccountNumberMask",
    "BankCode",
    "CreditCardMaskNumber",
    "CreditCardExpirationMonth",
    "CreditCardExpirationYear",
    "CreditCardType"
  )) { result =>
    PaymentMethod(
      id = result("Id"),
      `type` = result("Type"),
      numConsecutiveFailures = result.get("NumConsecutiveFailures").map(_.toInt),
      paymentMethodStatus = result.get("PaymentMethodStatus"),
      mandateId = result.get("MandateID"),
      tokenId = result.get("TokenId"),
      secondTokenId = result.get("SecondTokenId"),
      payPalEmail = result.get("PaypalEmail"),
      bankTransferType = result.get("BankTransferType"),
      bankTransferAccountName = result.get("BankTransferAccountName"),
      bankTransferAccountNumberMask = result.get("BankTransferAccountNumberMask"),
      bankCode = result.get("BankCode"),
      creditCardNumber = result.get("CreditCardMaskNumber").map(_ takeRight 4),
      creditCardExpirationMonth = result.get("CreditCardExpirationMonth"),
      creditCardExpirationYear = result.get("CreditCardExpirationYear"),
      creditCardType = result.get("CreditCardType")
    )
  }

  implicit val productReader = readers.Query("Product", Seq("Id", "Name")) { result =>
    Product(
      id = result("Id"),
      name = result("Name"))
  }

  implicit val ratePlanReader = readers.Query("RatePlan", Seq("Id", "Name", "ProductRatePlanId")) { result =>
    RatePlan(
      id = result("Id"),
      name = result("Name"),
      productRatePlanId = result("ProductRatePlanId"))
  }

  implicit val productRatePlanReader = readers.Query("ProductRatePlan",
    Seq("Id", "Name", "ProductId", "EffectiveStartDate", "EffectiveEndDate")) { result =>
    ProductRatePlan(
      id = result("Id"),
      name = result("Name"),
      productId = result("ProductId"),
      effectiveStartDate = new LocalDate(result("EffectiveStartDate")),
      effectiveEndDate = new LocalDate(result("EffectiveEndDate")))
  }

  implicit val productRatePlanChargeReader = readers.Query("ProductRatePlanCharge",
    Seq("Id", "Name", "ProductRatePlanId", "BillingPeriod")) { result =>
    ProductRatePlanCharge(
      id = result("Id"),
      name = result("Name"),
      productRatePlanId = result("ProductRatePlanId"),
      billingPeriod = result("BillingPeriod"))
  }

  implicit val productRatePlanChargeTierReader = readers.Query("ProductRatePlanChargeTier", Seq("Currency", "Price", "ProductRatePlanChargeId")) { result =>
    ProductRatePlanChargeTier(
      currency = result("Currency"),
      price = result("Price").toFloat,
      productRatePlanChargeId = result("ProductRatePlanChargeId"))
  }

  implicit val accountReader = readers.Query("Account", Seq("Id", "BillToId", "SoldToId", "BillCycleDay", "CreditBalance", "Currency", "DefaultPaymentMethodId", "sfContactId__c", "PaymentGateway")) { result =>
    Account(
      id = result("Id"),
      billToId = result("BillToId"),
      soldToId = result("SoldToId"),
      billCycleDay = result("BillCycleDay").toInt,
      creditBalance = result("CreditBalance").toFloat,
      currency = Currency.fromString(result("Currency")),
      defaultPaymentMethodId = result.get("DefaultPaymentMethodId"),
      sfContactId = result.get("sfContactId__c"),
      paymentGateway = result.get("PaymentGateway").flatMap(PaymentGateway.getByName)
    )
  }

  implicit val contactReader = readers.Query("Contact", Seq("Id", "FirstName", "LastName", "PostalCode", "Country", "WorkEmail")) { result =>
    Contact(
      id = result("Id"),
      firstName = result("FirstName"),
      lastName = result("LastName"),
      postalCode = result.get("PostalCode"),
      country = result.get("Country").flatMap(ZuoraLookup.country),
      email = result.get("WorkEmail")
    )
  }

  implicit val ratePlanChargeReader = readers.Query("RatePlanCharge", Seq(
    "Id", "ChargedThroughDate", "EffectiveStartDate", "BillingPeriod", "ChargeModel", "ChargeType", "Price"
  )) { result =>
    RatePlanCharge(
      id = result("Id"),
      chargedThroughDate = result.get("ChargedThroughDate").map(new LocalDate(_)),
      effectiveStartDate = new LocalDate(result("EffectiveStartDate")),
      billingPeriod = result.get("BillingPeriod"),
      chargeModel = result.get("ChargeModel"),
      chargeType = result.get("ChargeType"),
      price = result("Price").toFloat)
  }

  implicit val subscriptionReader = readers.Query("Subscription",
    Seq("Id", "Name", "AccountId", "Version", "TermStartDate", "TermEndDate", "ContractAcceptanceDate", "ActivationDate__c")) { result =>
    Subscription(
      id = result("Id"),
      name = result("Name"),
      accountId = result("AccountId"),
      result("Version").toInt,
      termStartDate = new LocalDate(result("TermStartDate")),
      termEndDate = new LocalDate(result("TermEndDate")),
      contractAcceptanceDate = new LocalDate(result("ContractAcceptanceDate")),
      activationDate = result.get("ActivationDate__c").map(d => new DateTime(d).toLocalDate)
    )
  }

  implicit val usageReader = readers.Query("Usage", Seq("Description")) { result =>
    Usage(result("Description"))
  }
}
