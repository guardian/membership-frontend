package com.gu.salesforce

import com.github.nscala_time.time.Imports._
import play.api.libs.functional.syntax._
import play.api.libs.json.JodaReads.DefaultJodaDateTimeReads._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.reflect.internal.util.StringOps

object ContactDeserializer {

  object Keys {
    //This object is also used for the keys when contacts are serialized.
    val CONTACT_ID = "Id"
    val ACCOUNT_ID = "AccountId"
    val TITLE = "Salutation" //Salesforce uses 'Salutation' field to store title.
    val FIRST_NAME = "FirstName"
    val LAST_NAME = "LastName"
    val IDENTITY_ID = "IdentityID__c"
    val DEFAULT_CARD_ID = "Stripe_Default_Card_ID__c"
    val STRIPE_CUSTOMER_ID = "Stripe_Customer_ID__c"
    val TIER = "Membership_Tier__c"
    val REG_NUMBER = "Membership_Number__c"
    val CREATED = "CreatedDate"
    val EMAIL = "Email"
    val BIRTH_DATE = "Birthdate"
    val GENDER = "Gender__c"
    val MAILING_STREET = "MailingStreet"
    val MAILING_CITY = "MailingCity"
    val MAILING_STATE = "MailingState"
    val MAILING_POSTCODE = "MailingPostalCode"
    val MAILING_COUNTRY = "MailingCountry"
    val BILLING_STREET = "OtherStreet"
    val BILLING_CITY = "OtherCity"
    val BILLING_STATE = "OtherState"
    val BILLING_POSTCODE = "OtherPostalCode"
    val BILLING_COUNTRY = "OtherCountry"
    val TELEPHONE = "Phone"
    val ALLOW_THIRD_PARTY_EMAIL = "Allow_3rd_Party_Mail__c"
    val ALLOW_GU_RELATED_MAIL = "Allow_Guardian_Related_Mail__c"
    val ALLOW_MEMBERSHIP_MAIL = "Allow_Membership_Mail__c"
    val DELIVERY_INSTRUCTIONS = "Delivery_Information__c"
    val RECORD_TYPE_ID = "RecordTypeId"
  }

  import Keys._
  implicit val sfDateReads = JodaReads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  implicit val contact: Reads[Contact] = (
      (__ \ IDENTITY_ID).readNullable[String] and
      (__ \ REG_NUMBER).readNullable[String] and
      (__ \ TITLE).readNullable[String] and
      (__ \ FIRST_NAME).readNullable[String] and
      (__ \ LAST_NAME).read[String] and
      (__ \ CREATED).read[DateTime] and
      (__ \ CONTACT_ID).read[String] and
      (__ \ ACCOUNT_ID).read[String] and
      (__ \ MAILING_STREET).readNullable[String] and
      (__ \ MAILING_CITY).readNullable[String] and
      (__ \ MAILING_STATE).readNullable[String] and
      (__ \ MAILING_POSTCODE).readNullable[String] and
      (__ \ MAILING_COUNTRY).readNullable[String] and
      (__ \ DELIVERY_INSTRUCTIONS).readNullable[String] and
      (__ \ RECORD_TYPE_ID).readNullable[String]
    ) (Contact)

}
