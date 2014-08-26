package model

import play.api.libs.json.Json


case class IdentityUser(id: String,
                        privateFields: PrivateFields,
                        statusFields: StatusFields,
                        passwordExists: Option[Boolean] = None )


//this can't be a Map[String,String] as PrivateFields in Identity has other object types
case class PrivateFields(firstName: Option[String] = None,
                         secondName: Option[String] = None,
                         address1: Option[String] = None,
                         address2: Option[String] = None,
                         address3: Option[String] = None,
                         address4: Option[String] = None,
                         postcode: Option[String] = None,
                         country: Option[String] = None,
                         billingAddress1: Option[String] = None,
                         billingAddress2: Option[String] = None,
                         billingAddress3: Option[String] = None,
                         billingAddress4: Option[String] = None,
                         billingPostcode: Option[String] = None,
                         billingCountry: Option[String] = None)

case class StatusFields(receiveGnmMarketing: Option[Boolean] = None,
                        receive3rdPartyMarketing: Option[Boolean] = None)

object UserDeserializer {
  implicit val readsStatusFields = Json.reads[StatusFields]
  implicit val readsPrivateFields = Json.reads[PrivateFields]
  implicit val readsUser = Json.reads[model.IdentityUser]
}