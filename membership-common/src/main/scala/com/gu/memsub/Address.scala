package com.gu.memsub

import com.gu.i18n.Country._
import com.gu.i18n.CountryGroup

case class Address(lineOne: String, lineTwo: String, town: String, countyOrState: String,
                   postCode: String, countryName: String) {
  // Salesforce only has one address line field, so merge our two together
  val line = Seq(lineOne, lineTwo).filter(_.nonEmpty).mkString(", ")

  lazy val valid = country.fold(false) {
    case c if List(US, Canada).contains(c) => postCode.nonEmpty && c.states.contains(countyOrState)
    case c if c == Ireland => lineOne.nonEmpty && town.nonEmpty
    case _ => postCode.nonEmpty
  }

  lazy val country = CountryGroup.countryByNameOrCode(countryName)
}
