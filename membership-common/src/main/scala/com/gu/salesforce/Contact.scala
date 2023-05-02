package com.gu.salesforce
import com.github.nscala_time.time.Imports._
import com.gu.i18n.{Country, CountryGroup}

import scala.language.implicitConversions

trait ContactId {
  def salesforceContactId: String
  def salesforceAccountId: String
}

case class Contact(
  identityId: Option[String],
  regNumber: Option[String],
  title: Option[String],
  firstName: Option[String],
  lastName: String,
  joinDate: DateTime,
  salesforceContactId: String,
  salesforceAccountId: String,
  mailingStreet: Option[String], // used for fulfilment
  mailingCity: Option[String],
  mailingState: Option[String],
  mailingPostcode: Option[String],
  mailingCountry: Option[String],
  deliveryInstructions: Option[String],
  recordTypeId: Option[String]
) extends ContactId
