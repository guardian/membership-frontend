package services.eventbrite

import configuration.Config
import model.Eventbrite.EBEvent
import model.RichEvent.{MasterclassEvent, RichEvent}
import monitoring.EventbriteMetrics
import services.GuardianContentService
import services.eventbrite.MasterclassEventsProvider.MasterclassesWithAvailableMemberDiscounts

import scala.concurrent.Future


object MasterclassEventCache extends EventbriteCache {
  val client = new EventbriteClient(Config.eventbriteMasterclassesApiToken, 1, new EventbriteMetrics("Masterclasses"))
  val contentApiService = GuardianContentService

  override def events: Seq[RichEvent] = super.events.filter(MasterclassesWithAvailableMemberDiscounts)

  def mkRichEvent(event: EBEvent): Future[RichEvent] = {
    val masterclassData = contentApiService.masterclassContent(event.id)
    //todo change this to have link to weburl
    Future.successful(MasterclassEvent(event, masterclassData))
  }

  override def getFeaturedEvents: Seq[RichEvent] = Nil

  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))
}

object MasterclassEventsProvider {
  val MasterclassesWithAvailableMemberDiscounts: (RichEvent) => Boolean =
    _.internalTicketing.exists(_.memberDiscountOpt.exists(!_.isSoldOut))
}

case class MasterclassEventServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}
