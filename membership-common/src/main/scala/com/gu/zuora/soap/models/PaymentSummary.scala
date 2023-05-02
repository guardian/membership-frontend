package com.gu.zuora.soap.models

import com.gu.i18n.Currency
import com.gu.memsub.Price
import com.gu.stripe.Stripe
import com.gu.zuora.soap.models.Queries.{InvoiceItem, PreviewInvoiceItem, Subscription}

case class PaymentSummary(current: InvoiceItem, previous: Seq[InvoiceItem], currency: Currency) {
  val totalPrice = current.price + previous.map(_.price).sum
}

object PaymentSummary {
  def apply(items: Seq[InvoiceItem], currency: Currency): PaymentSummary = {
    val sortedInvoiceItems = items.sortBy(_.chargeNumber)
    PaymentSummary(sortedInvoiceItems.last, sortedInvoiceItems.dropRight(1), currency)
  }
}
