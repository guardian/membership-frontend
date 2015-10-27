package services

import model.RichEvent._
import play.api.Play.current
import play.api.cache.Cache
import services.eventbrite.{EventbriteCache, GuardianLiveEventCache, LocalEventCache, MasterclassEventCache}

import scala.concurrent.Future

trait EventbriteCollectiveServices {
  val services = Seq(GuardianLiveEventCache, LocalEventCache, MasterclassEventCache)

  implicit class RichEventProvider(event: RichEvent) {
    val service = event match {
      case _: GuLiveEvent => GuardianLiveEventCache
      case _: LocalEvent => LocalEventCache
      case _: MasterclassEvent => MasterclassEventCache
    }
    val client = event.service.client
  }

  def getPreviewEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    GuardianLiveEventCache.getPreviewEvent(id)
  }

  def getPreviewLocalEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    LocalEventCache.getPreviewEvent(id)
  }

  def getPreviewMasterclass(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    MasterclassEventCache.getPreviewEvent(id)
  }

  def searchServices(fn: EventbriteCache => Option[RichEvent]): Option[RichEvent] =
    services.flatMap { service => fn(service) }.headOption

  def getBookableEvent(id: String) = searchServices(_.getBookableEvent(id))
  def getEvent(id: String) = searchServices(_.getEvent(id))
}

object EventbriteService extends EventbriteCollectiveServices
