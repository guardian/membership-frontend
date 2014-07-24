package model

import com.github.nscala_time.time.Imports._

import play.api.libs.json.{Reads, JsPath}
import play.api.libs.functional.syntax._

case class Member(salesforceContactId: String,
                  identityId: String,
                  tier: Tier.Tier,
                  stripeCustomerId: Option[String],
                  joinDate: DateTime,
                  optedIn: Boolean)

object Member {
  object Keys {
    val ID = "Id"
    val LAST_NAME = "LastName"
    val USER_ID = "IdentityID__c"
    val CUSTOMER_ID = "Stripe_Customer_ID__c"
    val TIER = "Membership_Tier__c"
    val OPT_IN = "Membership_Opt_In__c"
    val CREATED = "CreatedDate"
    val EMAIL = "Email"
  }
}

object MemberDeserializer {
  import Member._

  implicit val readsDateTime = JsPath.read[String].map(s => new DateTime(s))
  implicit val readsMember: Reads[Member] = (
    (JsPath \ Keys.ID).read[String] and
      (JsPath \ Keys.USER_ID).read[String] and
      (JsPath \ Keys.TIER).read[String].map(Tier.withName) and
      (JsPath \ Keys.CUSTOMER_ID).read[Option[String]] and
      (JsPath \ Keys.CREATED).read[DateTime] and
      (JsPath \ Keys.OPT_IN).read[Boolean]
    )(Member.apply _)
}
