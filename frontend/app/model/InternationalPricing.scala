package model

import com.gu.membership.zuora.rest._
import Function.const

case class InternationalPricing(pricingByCurrency: Map[Currency, Pricing]) {

  def forCurrency(currency: Currency): Option[Pricing] =
    pricingByCurrency.get(currency)
}

object InternationalPricing {
  val currencies: Set[Currency] = Set(GBP, USD, AUD, EUR)

  def fromPricingSummaries(monthly: PricingSummary,
                           yearly: PricingSummary): Option[InternationalPricing] = {
    val allPricing =
      currencies
        .map(c => (c, monthly.getPrice(c), yearly.getPrice(c)))
        .collect { case (c, Some(mPrice), Some(yPrice)) => Pricing(c, mPrice.toInt, yPrice.toInt)}

    allPricing.find(_.currency == GBP).map(const(
      InternationalPricing(allPricing.map(p => p.currency -> p).toMap)
    ))
  }
}
