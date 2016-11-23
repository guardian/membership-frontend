package utils

import com.gu.i18n.CountryGroup
import model.ActiveCountryGroups

object CountryGroupLang {
  val langByCountryGroup: Map[CountryGroup, String] = (
    for {
      countryGroup: CountryGroup <- ActiveCountryGroups.all
      defaultCountry <- countryGroup.defaultCountry
    } yield countryGroup -> s"en-${defaultCountry.alpha2.toLowerCase}"
  ).toMap[CountryGroup, String] + (CountryGroup.RestOfTheWorld -> "en")
}
