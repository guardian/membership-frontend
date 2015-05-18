package model

import com.github.nscala_time.time.Imports._
import model.Eventbrite._
import model.RichEvent.{ChooseTierMetadata, Metadata, RichEvent}
import org.joda.time.DateTime

object EventbriteTestObjects {
  def eventName(eventName: String = "Event Name") = EBRichText(eventName, "")
  def eventTime = DateTime.now()
  def eventDescription(description: String = "Event Description") = new EBRichText(description, "")
  def eventLocation = new EBAddress(None, None, None, None, None, None)
  def eventVenue = new EBVenue(Option(eventLocation), None)
  def eventWithName(name: String = "") = EBEvent(eventName(name), Option(eventDescription()), "", name, eventTime, eventTime + 2.hours, (eventTime - 1.month).toInstant, eventVenue, 0, Seq.empty, "live")
  def eventTicketClass = EBTicketClass("", "", false, 0, 0, None, None, eventTime.toInstant, None, None)

  case class TestRichEvent(event: EBEvent) extends RichEvent {
    val imgOpt = None
    val eventMetadata = None
    val socialImgUrl = None
    val socialHashTag = None
    val imageMetadata = None
    val schema = EventSchema.from(this)
    val tags = Nil
    val contentOpt = None
    val pastImageOpt = None

    val metadata = Metadata(
      identifier="",
      title="",
      shortTitle="",
      pluralTitle="",
      description=None,
      eventListUrl="",
      termsUrl="",
      largeImg=false,
      preSale=true,
      highlightsOpt=None,
      chooseTier=ChooseTierMetadata("", "")
    )

    def deficientGuardianMembersTickets: Boolean = false
  }

}
