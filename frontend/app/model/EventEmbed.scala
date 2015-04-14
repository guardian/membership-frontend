package model

import play.api.libs.json.Json

/*
 * used to provide event data to Composer for embedded Membership
 * events on theguardian.com articles – be wary of changing this
 * without changing those services, too.
 */
case class EmbedData(title: String,
                     image: Option[String],
                     venue: Option[String],
                     location: Option[String],
                     price: Option[String],
                     identifier: String,
                     start: String,
                     end: String)

case class EmbedResponse(status: String, result: Option[EmbedData])

object EmbedSerializer {
  implicit val writesEmbedData = Json.writes[EmbedData]
  implicit val writesResponse = Json.writes[EmbedResponse]

  def eventToJson(embedData: Option[EmbedData]) = {
    Json.toJson(EmbedResponse(embedData.fold("error")(_ => "success"), embedData))
  }
}
