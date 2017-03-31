package model

import com.gu.exacttarget.ContributionThankYouExtension
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}


case class ContributorRow(
                           email: String,
                           created: DateTime,
                           amount: BigDecimal,
                           currency: String,
                           name: String
                         ) {
  def edition: String = currency match {
    case "GBP" => "uk"
    case "USD" => "us"
    case "AUD" => "au"
    case _ => "international"
  }
}

object ContributorRow {

  implicit val contributorRowWriter = new Writes[ContributorRow] {
    def writes(c: ContributorRow): JsValue = Json.obj(
      "To" -> Json.obj(
        "Address" -> c.email,
        "SubscriberKey" -> c.email,
        "ContactAttributes" -> Json.obj(
          "SubscriberAttributes" -> Json.obj(
            "EmailAddress" -> c.email,
            "created" -> c.created.toString(),
            "amount" -> c.amount,
            "currency" -> c.currency,
            "edition" -> c.edition,
            "name" -> c.name
          )
        )
      ),
      "DataExtensionName" -> ContributionThankYouExtension.name
    )
  }
}





