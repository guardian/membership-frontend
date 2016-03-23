package services

import com.github.nscala_time.time.OrderingImplicits._
import com.gu.lib.okhttpscala._
import com.gu.memsub.util.{ScheduledTask, WebServiceHelper}
import com.squareup.okhttp.{OkHttpClient, Request}
import configuration.Config
import configuration.Config.Implicits.akkaSystem
import model.Eventbrite._
import model.EventbriteDeserializer._
import model.RichEvent._
import monitoring.EventbriteMetrics
import org.joda.time.{DateTime, Interval}
import play.api.Logger
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json.{Json, Reads}
import utils.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait EventbriteService extends WebServiceHelper[EBObject, EBError] {
  val apiToken: String
  val maxDiscountQuantityAvailable: Int

  override val wsUrl = Config.eventbriteApiUrl
  override def wsPreExecute(builder: Request.Builder): Request.Builder = {
    val req = builder.build()
    // Eventbrite redirects all request with no trailing "/"
    // /v3/users/me/owned_events => /v3/users/me/owned_events/
    // This is particularly bad for POST requests.
    //
    // In order to avoid this, we force okhttp to append a
    // trailing slash to the request path
    val url =
      req.httpUrl().newBuilder()
        .addPathSegment("")
        .addQueryParameter("token", apiToken).build()

    req.newBuilder().url(url)
  }

  def eventsTaskFor(status: String, initialDelay: FiniteDuration, refreshTime: FiniteDuration): ScheduledTask[Seq[RichEvent]] =
    ScheduledTask[Seq[RichEvent]](s"Eventbrite $status events", Nil, initialDelay, refreshTime) {
      for {
        events <- getAll[EBEvent]("users/me/owned_events/", List(
          "status" -> status,
          "expand" -> EBEvent.expansions.mkString(",")))
        richEvents <- Future.traverse(events)(mkRichEvent)
      } yield richEvents
    }

  // The prime numbers are to spread the requests out over the refreshTime.
  // Live needs to complete ASAP for healthcheck to pass.
  // Customers see ended in the archive page so it needs to happen soonish.
  // Draft is for a backend tool, so it can start once everything else has finished.
  // The HTTP client library in memcommon has a hardcoded timeout of 10 seconds, so the
  // goal here is to keep the downstream cache warm (AWS CloudFront - ttl 61s).
  lazy val eventsTask = eventsTaskFor("live", 1.second, Config.eventbriteRefreshTime.seconds)

  lazy val archivedEventsTask = eventsTaskFor("ended", 29.seconds, Config.eventbriteRefreshTime.seconds)

  lazy val draftEventsTask =  eventsTaskFor("draft", 59.seconds, Config.eventbriteRefreshTime.seconds)

  def start() {
    Logger.info(s"Starting EventbriteService background tasks for ${this.getClass.getSimpleName}")
    val timeout = (Config.eventbriteRefreshTime - 3).seconds
    eventsTask.start(timeout)
    draftEventsTask.start(timeout)
    archivedEventsTask.start(timeout)
  }

  def events: Seq[RichEvent] = eventsTask.get().filterNot(e => HiddenEvents.contains(e.id))
  def eventsDraft: Seq[RichEvent] = draftEventsTask.get()
  def eventsArchive: Seq[RichEvent] = archivedEventsTask.get()

  def mkRichEvent(event: EBEvent): Future[RichEvent]
  def getFeaturedEvents: Seq[RichEvent]
  def getTaggedEvents(tag: String): Seq[RichEvent] = Seq.empty
  def getEventsArchive: Option[Seq[RichEvent]] = Some(eventsArchive)
  def getPartnerEvents: Seq[RichEvent] = events.filter(_.providerOpt.isDefined)

  private def getAll[T](url: String, params: Seq[(String, String)] = Seq.empty)(implicit reads: Reads[EBResponse[T]]): Future[Seq[T]] = {
    def getPage(page: Int) = get[EBResponse[T]](url, Seq("page" -> page.toString) ++ params:_*)

    for {
      initialResponse <- getPage(1)
      followingResponses: Seq[EBResponse[T]] <- Future.traverse(2 to initialResponse.pagination.page_count)(getPage)
    } yield (initialResponse +: followingResponses).flatMap(_.data)
  }

  def getPreviewEvent(id: String): Future[RichEvent] = for {
    event <- get[EBEvent](s"events/$id/", "expand" -> EBEvent.expansions.mkString(","))
    richEvent <- mkRichEvent(event)
  } yield richEvent

  def getBookableEvent(id: String): Option[RichEvent] = events.find(_.id == id)
  def getEvent(id: String): Option[RichEvent] = (events ++ eventsArchive).find(_.id == id)

  def getEventsByIds(ids: Seq[String]): Seq[RichEvent] = events.filter(e => ids.contains(e.event.id))
  def getLimitedAvailability: Seq[RichEvent] = events.filter(_.event.isLimitedAvailability)
  def getRecentlyCreated(start: DateTime): Seq[RichEvent] = events.filter(_.created.isAfter(start))
  def getSortedByCreationDate: Seq[RichEvent] = events.sortBy(_.created.toDateTime)(Ordering[DateTime].reverse)
  def getEventsBetween(interval: Interval): Seq[RichEvent] = events.filter(event => interval.contains(event.start))

  def getEventsByLocation(slug: String): Seq[RichEvent] = events.filter(_.venue.address.flatMap(_.city).exists(c => slugify(c) == slug))

  def createOrGetAccessCode(event: RichEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[Option[EBAccessCode]] = {
      val uri = s"events/${event.id}/access_codes/"

      for {
        discounts <- getAll[EBAccessCode](uri) if ticketClasses.nonEmpty
        discount <- discounts.find(_.code == code).fold {
          post[EBAccessCode](uri, Map(
            "access_code.code" -> Seq(code),
            "access_code.quantity_available" -> Seq(maxDiscountQuantityAvailable.toString),
            "access_code.ticket_ids" -> Seq(ticketClasses.map(_.id).mkString(","))
          ))
        }(Future.successful)
      } yield Some(discount)
  } recover { case _: NoSuchElementException => None }

  def getOrder(id: String): Future[EBOrder] = get[EBOrder](s"orders/$id/", "expand" -> EBOrder.expansions.mkString(","))
}

abstract class LiveService extends EventbriteService {

  val contentApiService = GuardianContentService

  def gridImageFor(event: EBEvent) =
    event.mainImageGridId.fold[Future[Option[GridImage]]](Future.successful(None))(GridService.getRequestedCrop)
}

object GuardianLiveEventService extends LiveService {
  val apiToken = Config.eventbriteApiToken
  // For partner/patrons with free event tickets benefits, we generate a discount code which unlocks a combination of
  // maximum 2 discounted tickets and 1 complimentary ticket.
  // The maxDiscountQuantityAvailable value is used to set the Access code 'quantity_available' attribute (i.e. the
  // maximum number of tickets that can be purchased with a given code).
  //
  // see https://www.eventbrite.com/developer/v3/formats/event/#ebapi-access-code
  val maxDiscountQuantityAvailable = 4
  val wsMetrics = new EventbriteMetrics("Guardian Live")

  lazy val eventsOrderingTask = ScheduledTask[Seq[String]]("Event ordering", Nil, 1.second, Config.eventbriteRefreshTimeForPriorityEvents.seconds) {
    for {
      resp <- new OkHttpClient().execute(new Request.Builder().url(Config.eventOrderingJsonUrl).build())
    } yield (Json.parse(resp.body.string) \ "order").as[Seq[String]]
  }

  def mkRichEvent(event: EBEvent): Future[RichEvent] = for { gridImageOpt <- gridImageFor(event) }
    yield GuLiveEvent(event, gridImageOpt, contentApiService.content(event.id))

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(eventsOrderingTask.get(), events)
  override def start() {
    super.start()
    Logger.info("Starting EventsOrdering background task")
    val timeout = (Config.eventbriteRefreshTimeForPriorityEvents - 3).seconds
    eventsOrderingTask.start(timeout)
  }
}

object LocalEventService extends LiveService {
  val apiToken = Config.eventbriteLocalApiToken
  val maxDiscountQuantityAvailable = 2
  val wsMetrics = new EventbriteMetrics("Local")

  def mkRichEvent(event: EBEvent): Future[RichEvent] =  for { gridImageOpt <- gridImageFor(event) }
    yield LocalEvent(event, gridImageOpt, contentApiService.content(event.id))

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(Nil, events)
}

case class MasterclassEventServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object MasterclassEventsProvider {
  val MasterclassesWithAvailableMemberDiscounts: (RichEvent) => Boolean =
    _.internalTicketing.exists(_.memberDiscountOpt.exists(!_.isSoldOut))
}

object MasterclassEventService extends EventbriteService {
  import MasterclassEventsProvider._

  val apiToken = Config.eventbriteMasterclassesApiToken
  val maxDiscountQuantityAvailable = 1

  val wsMetrics = new EventbriteMetrics("Masterclasses")

  val contentApiService = GuardianContentService

  override def events: Seq[RichEvent] =
    super.events.filter(MasterclassesWithAvailableMemberDiscounts)

  def mkRichEvent(event: EBEvent): Future[RichEvent] = {
    val masterclassData = contentApiService.masterclassContent(event.id)
    //todo change this to have link to weburl
    Future.successful(MasterclassEvent(event, masterclassData))
  }

  override def getFeaturedEvents: Seq[RichEvent] = Nil
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))
}

object EventbriteServiceHelpers {

  def getFeaturedEvents(orderedIds: Seq[String], events: Seq[RichEvent]): Seq[RichEvent] = {
    val (orderedEvents, normalEvents) = events.partition { event => orderedIds.contains(event.id) }
    orderedEvents.sortBy { event => orderedIds.indexOf(event.id) } ++ normalEvents.filter(!_.isSoldOut).take(4 - orderedEvents.length)
  }
}

trait EventbriteCollectiveServices {
  val services = Seq(GuardianLiveEventService, LocalEventService, MasterclassEventService)

  implicit class RichEventProvider(event: RichEvent) {
    val service = event match {
      case _: GuLiveEvent => GuardianLiveEventService
      case _: LocalEvent => LocalEventService
      case _: MasterclassEvent => MasterclassEventService
    }
  }

  def getPreviewEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    GuardianLiveEventService.getPreviewEvent(id)
  }

  def getPreviewLocalEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    LocalEventService.getPreviewEvent(id)
  }

  def getPreviewMasterclass(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    MasterclassEventService.getPreviewEvent(id)
  }

  def searchServices(fn: EventbriteService => Option[RichEvent]): Option[RichEvent] =
    services.flatMap { service => fn(service) }.headOption

  def getBookableEvent(id: String): Option[RichEvent] = searchServices(_.getBookableEvent(id))
  def getEvent(id: String): Option[RichEvent] = searchServices(_.getEvent(id))
}

object EventbriteService extends EventbriteCollectiveServices
