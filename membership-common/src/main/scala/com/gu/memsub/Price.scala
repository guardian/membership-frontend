package com.gu.memsub

import com.gu.i18n.Currency

import scala.math.BigDecimal.RoundingMode.HALF_UP

case class Price(amount: Float, currency: Currency) {
  val prettyAmount = {
    val rounded2dp = BigDecimal(amount.toString).setScale(2, HALF_UP)
    rounded2dp.toBigIntExact.fold(rounded2dp.formatted("%.2f"))(_.toString)
  }

  val pretty = currency.identifier + prettyAmount
  val prettyWithoutCurrencyPrefix = currency.glyph + prettyAmount
  def +(n: Float) = Price(amount + n, currency)
  def -(n: Float) = Price(amount - n, currency)
  def *(n: Float) = Price(amount * n, currency)
  def /(n: Float) = Price(amount / n, currency)

  private def checkingCurrency(that: Price, op: (Float, Float) => Float): Price = {
    assert(that.currency == currency, s"Trying to compute a price from prices with different currencies: $currency and ${that.currency}")
    Price(op(amount, that.amount), currency)
  }

  def +(that: Price) = checkingCurrency(that, _ + _)
  def -(that: Price) = checkingCurrency(that, _ - _)
  def *(that: Price) = checkingCurrency(that, _ * _)
  def /(that: Price) = checkingCurrency(that, _ / _)
}
