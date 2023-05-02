package com.gu.zuora.api

import com.gu.i18n.{Country, CountryGroup}
import com.typesafe.config.Config

case class InvoiceTemplate(id: String, country: Country)

object InvoiceTemplates {
  import scala.util.Try

  def fromConfig(config: Config): List[InvoiceTemplate] =
    CountryGroup.countries.flatMap { country =>
      Try(config.getString(country.alpha2)).toOption.map(InvoiceTemplate(_, country))
    }
}