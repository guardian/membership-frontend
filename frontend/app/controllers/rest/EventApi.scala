package controllers.rest

import controllers._
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Controller
import services.GuardianLiveEventService

object EventApi {
  case class EventsResponse(events: Seq[Event])

  object EventsResponse {
    implicit val writesEventsData = Json.writes[EventsResponse]
  }
}

class EventApi() extends Controller with LazyLogging {

  import EventApi._

  /**
    * @return for now, only Guardian Live Events - other types may be added in the future
    */
  def events = CachedAction {
    Ok(toJson(EventsResponse(GuardianLiveEventService.events.map(Event.forRichEvent(_)))))
  }
}
