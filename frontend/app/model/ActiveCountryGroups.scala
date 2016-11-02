package model


import com.gu.i18n.Country
import com.gu.i18n.CountryGroup._

object ActiveCountryGroups {
  val all = Seq(UK, US, Australia, Canada, Europe.copy(defaultCountry = Some(Country.Ireland)), RestOfTheWorld)
}
