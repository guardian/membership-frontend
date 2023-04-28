package com.gu.memsub

import com.gu.i18n.Currency
import scalaz.Semigroup

case class PricingSummary(underlying: Map[Currency, Price]) {
  def getPrice(k: Currency): Option[Price] = underlying.get(k)
  def nonEmpty: Boolean = !isEmpty
  def isEmpty: Boolean = underlying.isEmpty
  val prices = underlying.values
  val currencies = underlying.keySet
  val isFree = prices.map(_.amount).sum == 0
}

object PricingSummary {
  implicit object PricingSemigroup extends Semigroup[PricingSummary] {
    override def append(f1: PricingSummary, f2: => PricingSummary): PricingSummary =
      PricingSummary(f1.underlying.keySet.intersect(f2.underlying.keySet).map(c => c -> Price(f1.underlying(c).amount + f2.underlying(c).amount, c)).toMap)
  }
}
