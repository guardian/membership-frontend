package controllers

import _root_.model.EventbriteEvent.{EBResponse, EBEvent}
import play.api._
import play.api.mvc._
import com.stripe._
import com.stripe.model._
import scala.collection.convert.wrapAll._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
