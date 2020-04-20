package utils

import model.Eventbrite.{EBDescription, EBEvent, EventWithDescription}

object Implicits {

  implicit class Cheat(event: EBEvent) {
    // this is just a way to get legacy events from the test data into the new format
    // the proper way would be to recreate in eventbrite, and run the actual api calls to get it
    def toAssumedEventWithDescription: EventWithDescription =
      EventWithDescription(event, EBDescription(event.description.map(_.html).getOrElse("oops, missing description EBEventTest")))
  }

}
