package model

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import play.api.libs.json.Json
import utils.awswrappers.dynamodb._

case class EventMetadata(
  ticketingProvider: String,
  ticketingProviderId: String,
  gridUrl: Option[String]
)

object EventMetadata {

  implicit val jsonWrites = Json.writes[EventMetadata]

  def fromAttributeValueMap(xs: Map[String, AttributeValue]) = {
    for {
      ticketingProvider <- xs.getString("ticketingProvider")
      ticketingProviderId <- xs.getString("ticketingProviderId")
    } yield EventMetadata(
      ticketingProvider,
      ticketingProviderId,
      xs.getString("gridUrl")
    )
  }
}
