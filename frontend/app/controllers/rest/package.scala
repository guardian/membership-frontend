package controllers

import com.netaporter.uri.Uri
import org.joda.time.DateTimeZone.UTC
import org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis
import org.joda.time.DateTime
import play.api.libs.json.{Json, JsString, JsValue, Writes}

package object rest {
  implicit val uriWrites = new Writes[Uri] {
    override def writes(uri: Uri): JsValue = JsString(uri.toString)
  }

  implicit val dateTimeWrites = new Writes[DateTime] {
    override def writes(dt: DateTime): JsValue =
      Json.obj(
          "time" -> dt.withZone(UTC).toString(dateTimeNoMillis),
          "zone" -> dt.getZone.getID
      )
  }
}
