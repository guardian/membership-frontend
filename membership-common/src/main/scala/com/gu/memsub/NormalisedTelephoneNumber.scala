package com.gu.memsub

import com.google.i18n.phonenumbers.{NumberParseException, PhoneNumberUtil}
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import com.gu.i18n.Country
import play.api.libs.json.Json

import scala.util.control.Exception._

case class NormalisedTelephoneNumber(countryCode:String, localNumber: String) {
  def format:String = s"+$countryCode$localNumber"
}

object NormalisedTelephoneNumber {
  def fromStringAndCountry(phone: Option[String], country: Option[Country]): Option[NormalisedTelephoneNumber] = {
    for {
      number <- phone
      c <- country
      parsed <- parseToOption(number, c.alpha2)
    } yield {
      NormalisedTelephoneNumber(parsed.getCountryCode.toString, parsed.getNationalNumber.toString)
    }
  }

  private def parseToOption(phone: String, countryCode: String): Option[PhoneNumber] = {
    val phoneNumberUtil = PhoneNumberUtil.getInstance()
    catching(classOf[NumberParseException]).opt(phoneNumberUtil.parse(phone, countryCode)).filter(phoneNumberUtil.isValidNumber)
  }
  implicit val writesTelephoneNumber = Json.writes[NormalisedTelephoneNumber]

}