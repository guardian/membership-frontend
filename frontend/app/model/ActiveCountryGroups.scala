package model


import com.gu.i18n.Country
import com.gu.i18n.CountryGroup._

object ActiveCountryGroups {
  val all = Seq(UK, Europe.copy(defaultCountry = Some(Country.Ireland)), US, Canada, Australia, RestOfTheWorld)
}
