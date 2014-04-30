package controllers

import model.EventbriteDeserializer._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import model.EBEvent

object EventController extends Controller {

  def getEventDetails(id: String) = {
    val url = s"https://www.eventbriteapi.com/v3/events/$id/?token=***REMOVED***"

    val request = WS.url(url).withQueryString(("token", "***REMOVED***"))

    for {
      response <- request.get()
    } yield {
      response.json.as[EBEvent]
    }
  }

  def renderEventPage(id: String) = Action.async {

    for {
      event <- getEventDetails(id)
    } yield {
      Ok(views.html.events.eventPage(event))
    }

  }

}
