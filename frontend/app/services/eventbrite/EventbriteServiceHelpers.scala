package services.eventbrite

import model.RichEvent.RichEvent


object EventbriteServiceHelpers {

   def getFeaturedEvents(orderedIds: Seq[String], events: Seq[RichEvent]): Seq[RichEvent] = {
     val (orderedEvents, normalEvents) = events.partition { event => orderedIds.contains(event.id) }
     orderedEvents.sortBy { event => orderedIds.indexOf(event.id) } ++ normalEvents.filter(!_.isSoldOut).take(4 - orderedEvents.length)
   }
 }
