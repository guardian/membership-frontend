package model

import org.joda.time.{ Instant, DateTime }
import model.Eventbrite._

object EventbriteTestObjects {
  def eventName(eventName: String = "Event Name") = EBRichText(eventName, "")
  def eventTime = DateTime.now()
  def eventDescription(description: String = "Event Description") = new EBRichText(description, "")
  def eventAddress = new EBAddress(None, None, None, None, None, None)
  def eventVenue = new EBVenue(None, Option(eventAddress), None, None, None)
  def eventWithName(name: String = "") = EBEvent(eventName(name), Option(eventDescription()), Option(""), "", "", eventTime, eventTime, eventVenue, None, Seq.empty, None)
}
