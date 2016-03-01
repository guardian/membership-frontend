package controllers

import com.typesafe.scalalogging.LazyLogging
import model.RichEvent.RichEvent
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Controller
import services.GridService.ImageIdWithCrop

object EventApi extends Controller with LazyLogging {
  case class Event(
    id: String,
    url: String,
    mainImage: Option[ImageIdWithCrop]
  )

  object Event {
    implicit val writesEvent = Json.writes[Event]

    def forRichEvent(e: RichEvent) = Event(e.id, e.memUrl, e.mainImageGridId)
  }

  case class EventsResponse(events: Seq[Event])

  object EventsResponse {
    implicit val writesEventsData = Json.writes[EventsResponse]
  }

  /**
    * @return for now, only Guardian Live Events - other types may be added in the future
    */
  def events = CachedAction {
    Ok(toJson(EventsResponse(controllers.Event.guLiveEvents.events.map(Event.forRichEvent))))
  }

}
