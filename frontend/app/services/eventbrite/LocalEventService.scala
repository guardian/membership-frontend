package services.eventbrite

import configuration.Config
import model.Eventbrite.EBEvent
import model.RichEvent.{LocalEvent, RichEvent}
import monitoring.EventbriteMetrics

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object LocalEventService extends LiveService {
   val apiToken = Config.eventbriteLocalApiToken
   val maxDiscountQuantityAvailable = 2
   val wsMetrics = new EventbriteMetrics("Local")

   def mkRichEvent(event: EBEvent): Future[RichEvent] =  for { gridImageOpt <- gridImageFor(event) }
     yield LocalEvent(event, gridImageOpt, contentApiService.content(event.id))

   override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(Nil, events)

   override def start() {
     super.start()
   }
 }
