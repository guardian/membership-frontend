package model

import com.github.nscala_time.time.Imports._
import model.Eventbrite._
import org.joda.time.DateTime

object EventbriteTestObjects {
  def eventName(eventName: String = "Event Name") = EBRichText(eventName, "")
  def eventTime = DateTime.now()
  def eventDescription(description: String = "Event Description") = new EBRichText(description, "")
  def eventLocation = new EBLocation(None, None, None, None, None, None, None, None)
  def eventVenue = new EBVenue(None, Option(eventLocation), None)
  def eventWithName(name: String = "") = EBEvent(eventName(name), Option(eventDescription()), Option(""), "", "", eventTime, eventTime, (eventTime - 1.month).toInstant, eventVenue, None, Seq.empty, "live")
}
