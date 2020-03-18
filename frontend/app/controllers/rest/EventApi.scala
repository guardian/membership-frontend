package controllers.rest

import actions.CommonActions
import controllers._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.{BaseController, ControllerComponents}
import services.{EventbriteCollectiveServices, GuardianLiveEventService}

object EventApi {
  case class EventsResponse(events: Seq[Event])

  object EventsResponse {
    implicit val writesEventsData = Json.writes[EventsResponse]
  }
}

class EventApi(eventbriteService: EventbriteCollectiveServices, commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CorsPublicCachedAction
  import EventApi._

  /**
    * @return for now, only Guardian Live Events - other types may be added in the future
    */
  def liveEvents = CorsPublicCachedAction {
    Ok(toJson(EventsResponse(eventbriteService.guardianLiveEventService.events.map(Event.forRichEvent(_)))))
  }

  def masterclassEvents = CorsPublicCachedAction {
    Ok(toJson(EventsResponse(eventbriteService.masterclassEventService.events.map(Event.forRichEvent(_)))))
  }
}
