package model

import com.github.nscala_time.time.Imports._
import model.Eventbrite._
import org.joda.time.DateTime

object EventbriteTestObjects {
  def eventName(eventName: String = "Event Name") = EBRichText(eventName, "")
  def eventTime = DateTime.now()
  def eventDescription(description: String = "Event Description") = new EBRichText(description, "")
  def eventLocation = new EBAddress(None, None, None, None, None, None)
  def eventVenue = new EBVenue(Option(eventLocation), None)
  def eventWithName(name: String = "") = EBEvent(eventName(name), Option(eventDescription()), "", "", eventTime, (eventTime - 1.month).toInstant, eventVenue, 0, Seq.empty, "live")
  def eventTickets = EBTickets("", "", false, 0, 0, None, eventTime.toInstant, None, None)
}
