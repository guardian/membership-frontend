package services.eventbrite

import com.gu.membership.util.ScheduledTask
import configuration.Config
import model.Eventbrite.EBEvent
import model.RichEvent.{GuLiveEvent, RichEvent}
import monitoring.EventbriteMetrics
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.current


object GuardianLiveEventCache extends LiveEventCache {
  // For partner/patrons with free event tickets benefits, we generate a discount code which unlocks a combination of
  // maximum 2 discounted tickets and 1 complimentary ticket.
  // The maxDiscountQuantityAvailable value is used to set the Access code 'quantity_available' attribute (i.e. the
  // maximum number of tickets that can be purchased with a given code).
  //
  // see https://www.eventbrite.com/developer/v3/formats/event/#ebapi-access-code
  val maxDiscountQuantityAvailable = 3
  val client = new EventbriteClient(Config.eventbriteApiToken, maxDiscountQuantityAvailable, new EventbriteMetrics("Guardian Live"))
  val refreshTimePriorityEvents = new FiniteDuration(Config.eventbriteRefreshTimeForPriorityEvents, SECONDS)

  lazy val eventsOrderingTask = ScheduledTask[Seq[String]]("Event ordering", Nil, 1.second, refreshTimePriorityEvents) {
    for {
      ordering <- WS.url(Config.eventOrderingJsonUrl).get()
    } yield (ordering.json \ "order").as[Seq[String]]
  }

  def mkRichEvent(event: EBEvent): Future[RichEvent] = for { gridImageOpt <- gridImageFor(event) }
    yield GuLiveEvent(event, gridImageOpt, contentApiService.content(event.id))

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteCacheHelpers.getFeaturedEvents(eventsOrderingTask.get(), events)
  override def start() {
    super.start()
    eventsOrderingTask.start()
  }
}
