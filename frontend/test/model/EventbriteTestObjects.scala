package model

import com.github.nscala_time.time.Imports._
import model.Eventbrite._
import model.RichEvent.RichEvent
import model.EventMetadata.{ChooseTierMetadata, Metadata}
import org.joda.time.DateTime

object EventbriteTestObjects {
  def eventName(eventName: String = "Event Name") = EBRichText(eventName, "")
  def eventTime = DateTime.now()
  def eventDescription(description: String = "Event Description") = new EBRichText(description, "")
  def eventLocation = new EBAddress(None, None, None, None, None, None)
  def eventVenue = new EBVenue(Option(eventLocation), None)
  def eventTicketClass = EBTicketClass("", "", None, false, 0, 0, None, None, None, eventTime.toInstant, None, None)
  def eventWithName(name: String = "") = EBEvent(eventName(name), Option(eventDescription()), "", name, eventTime, eventTime + 2.hours, (eventTime - 1.month).toInstant, eventVenue, 0, Seq(eventTicketClass), "live")

  case class TestRichEvent(event: EBEvent) extends RichEvent {
    val detailsUrl = ""
    val imgOpt = None
    val logoOpt = None
    val socialImgUrl = None
    val imageMetadata = None
    val schema = EventSchema.from(this)
    val tags = Nil
    val contentOpt = None
    val pastImageOpt = None
    val hasLargeImage = true
    val canHavePriorityBooking = true
    val gridImgUrl = None

    val metadata = Metadata(
      identifier="",
      title="",
      shortTitle="",
      pluralTitle="",
      description=None,
      socialHashtag=None,
      eventListUrl="",
      termsUrl="",
      highlightsOpt=None,
      chooseTier=ChooseTierMetadata("", "")
    )

    def deficientGuardianMembersTickets: Boolean = false
  }

}
