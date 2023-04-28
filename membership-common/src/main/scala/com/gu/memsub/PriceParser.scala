package com.gu.memsub

import com.gu.i18n.Currency

import scala.util.Try

object PriceParser {
  def parse(s: String): Option[Price] =
    s.replace("/Each", "").splitAt(3) match { case (code, p) =>
      for {
        currency <- Currency.fromString(code)
        price <- Try { p.toFloat }.toOption
      } yield Price(price, currency)
    }

}
