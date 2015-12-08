package views.support

import com.gu.i18n
import com.gu.i18n.Currency

case class CountryWithCurrency(country: i18n.Country, currency: i18n.Currency)

object CountryWithCurrency {
  val all = i18n.CountryGroup.allGroups.flatMap(group =>
    group.countries
      .map(c => CountryWithCurrency(c, group.currency)))
      .sortBy(_.country.name)

  def withCurrency(c: Currency) = all.map(_.copy(currency = c))

  def whitelisted(available: Set[Currency], default: Currency) =
    all.map { cc =>
      val currency = available.find(_ == cc.currency).getOrElse(default)
      cc.copy(currency = currency)
    }
}
