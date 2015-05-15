package model

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import play.api.libs.json.Json
import utils.awswrappers.dynamodb._

case class EventMetadata(
  id: String,
  eventId: String,
  gridUrl: Option[String]
)

object EventMetadata {

  implicit val jsonWrites = Json.writes[EventMetadata]

  def fromAttributeValueMap(xs: Map[String, AttributeValue]) = {
    for {
      id <- xs.getString("id")
      eventId <- xs.getString("event_id")
      externallyTicketed <- xs.get("externally_ticketed")
    } yield EventMetadata(
      id,
      eventId,
      xs.getString("grid_url")
    )
  }
}
