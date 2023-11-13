package services

import akka.actor.ActorSystem
import com.github.nscala_time.time.OrderingImplicits._
import com.gu.memsub.util.{ScheduledTask, WebServiceHelper}
import com.gu.okhttp.RequestRunners
import com.gu.okhttp.RequestRunners.FutureHttpClient
import configuration.Config
import model.Eventbrite._
import model.EventbriteDeserializer._
import model.RichEvent._
import okhttp3.Request
import org.joda.time.{DateTime, Interval}
import com.gu.monitoring.SafeLogger
import play.api.cache.AsyncCacheApi
import play.api.libs.json.{Json, Reads}
import utils.StringUtils._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class OrganisationId(value: String) extends AnyVal
case class EBAccount(apiToken: String, organisationId: OrganisationId)

object EBAccount {
  val live = EBAccount(
    Config.eventbriteApiToken,
    OrganisationId("104062388105")
  )
  val masterclass = EBAccount(
    Config.eventbriteMasterclassesApiToken,
    OrganisationId("7266571413")
  )
}
abstract class EventbriteService(ebAccount: EBAccount)(
  implicit val ec: ExecutionContext,
  system: ActorSystem
) extends WebServiceHelper[EBObject, EBError] {

  import ebAccount._
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
      req.url().newBuilder()
        .addPathSegment("")
        .addQueryParameter("token", apiToken).build()

    req.newBuilder().url(url)
  }

  def eventsTaskFor(status: String, initialDelay: FiniteDuration, refreshTime: FiniteDuration): ScheduledTask[Seq[RichEvent]] =
    ScheduledTask[Seq[RichEvent]](s"Eventbrite $status events", Nil, initialDelay, refreshTime) {
      for {
        events <- getAll[EBEvent](
          s"organizations/${organisationId.value}/events/",
          List(
            "status" -> status,
            "expand" -> EBEvent.expansions.mkString(",")
          )
        )
        eventDesc <- Future.traverse(events) { event =>
          getDescription(event.id).map { desc =>
            EventWithDescription(event, desc)
          }
        }
        richEvents <- Future.traverse(eventDesc)(mkRichEvent)
      } yield richEvents
    }

  // The prime numbers are to spread the requests out over the refreshTime.
  // Live needs to complete ASAP for healthcheck to pass.
  // Customers see ended in the archive page so it needs to happen soonish.
  // Draft is for a backend tool, so it can start once everything else has finished.
  // The HTTP client library in memcommon has a hardcoded timeout of 10 seconds, so the
  // goal here is to keep the downstream cache warm (AWS CloudFront - ttl 61s).
  lazy val eventsTask = eventsTaskFor("live", 1.second, Config.eventbriteRefreshTime.seconds)

  lazy val archivedEventsTask = eventsTaskFor("ended", 29.seconds, 7200.seconds)

  lazy val draftEventsTask =  eventsTaskFor("draft", 59.seconds, Config.eventbriteRefreshTime.seconds)

  def start() = {
    SafeLogger.info(s"Starting EventbriteService background tasks for ${this.getClass.getSimpleName}")
    eventsTask.start()
    draftEventsTask.start()
    archivedEventsTask.start()
  }

  def events: Seq[RichEvent] = eventsTask.get().filterNot(e => HiddenEvents.contains(e.underlying.ebEvent.id))
  def eventsDraft: Seq[RichEvent] = draftEventsTask.get()
  def eventsArchive: Seq[RichEvent] = archivedEventsTask.get()

  def mkRichEvent(event: EventWithDescription): Future[RichEvent]
  def getFeaturedEvents: Seq[RichEvent]
  def getTaggedEvents(tag: String): Seq[RichEvent] = Seq.empty
  def getEventsArchive: Option[Seq[RichEvent]] = Some(eventsArchive)
  def getPartnerEvents: Seq[RichEvent] = events.filter(_.underlying.ebDescription.providerOpt.isDefined)

  private def getAll[T](url: String, params: Seq[(String, String)] = Seq.empty)(implicit reads: Reads[EBResponse[T]]): Future[Seq[T]] = {
    def getPage(page: Int) = get[EBResponse[T]](url, Seq("page" -> page.toString) ++ params:_*)

    for {
      initialResponse <- getPage(1)
      followingResponses: Seq[EBResponse[T]] <- Future.traverse(2 to initialResponse.pagination.page_count)(getPage)
    } yield (initialResponse +: followingResponses).flatMap(_.data)
  }

  def getPreviewEvent(id: String): Future[RichEvent] = for {
    event <- get[EBEvent](s"events/$id/", "expand" -> EBEvent.expansions.mkString(","))
    desc <- getDescription(event.id)
    richEvent <- mkRichEvent(EventWithDescription(event, desc))
  } yield richEvent

  def getDescription(id: String): Future[EBDescription] =
    get[EBDescription](s"events/$id/description/")

  def getBookableEvent(id: String): Option[RichEvent] = events.find(_.underlying.ebEvent.id == id)
  def getEvent(id: String): Option[RichEvent] = (events ++ eventsArchive).find(_.underlying.ebEvent.id == id)

  def getEventsByIds(ids: Seq[String]): Seq[RichEvent] = events.filter(e => ids.contains(e.underlying.ebEvent.id))
  def getLimitedAvailability: Seq[RichEvent] = events.filter(_.underlying.isLimitedAvailability)
  def getRecentlyCreated(start: DateTime): Seq[RichEvent] = events.filter(_.underlying.ebEvent.created.isAfter(start))
  def getSortedByCreationDate: Seq[RichEvent] = events.sortBy(_.underlying.ebEvent.created.toDateTime)(Ordering[DateTime].reverse)
  def getEventsBetween(interval: Interval): Seq[RichEvent] = events.filter(event => interval.contains(event.underlying.ebEvent.start))

  def getEventsByLocation(slug: String): Seq[RichEvent] = events.filter(_.underlying.ebEvent.venue.address.flatMap(_.city).exists(c => slugify(c) == slug))

  def createOrGetAccessCode(event: RichEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[Option[EBAccessCode]] = {
      val uri = s"events/${event.underlying.ebEvent.id}/access_codes/"

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

class GuardianLiveEventService(
  executionContext: ExecutionContext,
  actorSystem: ActorSystem,
  contentApiService: GuardianContentService,
  gridService: GridService
) extends EventbriteService(EBAccount.live)(executionContext, actorSystem) {

  implicit private val as = actorSystem

  // For partner/patrons with free event tickets benefits, we generate a discount code which unlocks a combination of
  // maximum 2 discounted tickets and 1 complimentary ticket.
  // The maxDiscountQuantityAvailable value is used to set the Access code 'quantity_available' attribute (i.e. the
  // maximum number of tickets that can be purchased with a given code).
  //
  // see https://www.eventbrite.com/developer/v3/formats/event/#ebapi-access-code
  override val maxDiscountQuantityAvailable = 4

  override val httpClient: FutureHttpClient = RequestRunners.futureRunner

  private def getJson(url: String) = {
    val req = new Request.Builder().url(url).build()
    httpClient(req).map { response =>
      val responseBody = response.body.string()
      Json.parse(responseBody)
    }
  }

  lazy val eventsOrderingTask = ScheduledTask[Seq[String]]("Event ordering", Nil, 1.second, Config.eventbriteRefreshTimeForPriorityEvents.seconds) {
    for {
      ordering <- getJson(Config.eventOrderingJsonUrl)
    } yield (ordering \ "order").as[Seq[String]]
  }

  def gridImageFor(event: EventWithDescription) =
    event.mainImageGridId.fold[Future[Option[GridImage]]](Future.successful(None))(gridService.getRequestedCrop)

  override def mkRichEvent(event: EventWithDescription): Future[RichEvent] = for {gridImageOpt <- gridImageFor(event)}
    yield GuLiveEvent(event, gridImageOpt, contentApiService.content(event.ebEvent.id))

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(eventsOrderingTask.get(), events)
  override def start() = {
    super.start()
    SafeLogger.info("Starting EventsOrdering background task")
    val timeout = (Config.eventbriteRefreshTimeForPriorityEvents - 3).seconds
    eventsOrderingTask.start()
  }
}

case class MasterclassEventServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

class MasterclassEventService(
  executionContext: ExecutionContext,
  actorSystem: ActorSystem,
  contentApiService: GuardianContentService
) extends EventbriteService(EBAccount.masterclass)(executionContext: ExecutionContext, actorSystem: ActorSystem) {

  implicit private val as = actorSystem

  override val maxDiscountQuantityAvailable = 1

  override val httpClient: FutureHttpClient = RequestRunners.futureRunner

  override def mkRichEvent(event: EventWithDescription): Future[RichEvent] = {
    val masterclassData = contentApiService.masterclassContent(event.ebEvent.id)
    //todo change this to have link to weburl
    Future.successful(MasterclassEvent(event, masterclassData))
  }

  override def getFeaturedEvents: Seq[RichEvent] = Nil
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))
}

object EventbriteServiceHelpers {

  def getFeaturedEvents(orderedIds: Seq[String], events: Seq[RichEvent]): Seq[RichEvent] = {
    val (orderedEvents, normalEvents) = events.partition { event => orderedIds.contains(event.underlying.ebEvent.id) }
    orderedEvents.sortBy { event => orderedIds.indexOf(event.underlying.ebEvent.id) } ++ normalEvents
      .filterNot(_.underlying.isSoldOut)
      // Manual override to remove the Football Weekly Live tour events which have their own sale page on www
      .filterNot(_.underlying.ebEvent.name.text.contains("Football Weekly Live tour"))
      .take(4 - orderedEvents.length)
  }
}

object EventbriteService {
  implicit class RichEventProvider(event: RichEvent) {
    def service(implicit services: EventbriteCollectiveServices) = event match {
      case _: GuLiveEvent => services.guardianLiveEventService
      case _: MasterclassEvent => services.masterclassEventService
    }
  }
}

class EventbriteCollectiveServices(val cache: AsyncCacheApi, val guardianLiveEventService: GuardianLiveEventService, val masterclassEventService: MasterclassEventService) {
  lazy val services = Seq(guardianLiveEventService, masterclassEventService)

  def getPreviewEvent(id: String): Future[RichEvent] = cache.getOrElseUpdate[RichEvent](s"preview-event-$id", 2.seconds) {
    guardianLiveEventService.getPreviewEvent(id)
  }

  def getPreviewMasterclass(id: String): Future[RichEvent] = cache.getOrElseUpdate[RichEvent](s"preview-event-$id", 2.seconds) {
    masterclassEventService.getPreviewEvent(id)
  }

  def searchServices(fn: EventbriteService => Option[RichEvent]): Option[RichEvent] =
    services.flatMap { service => fn(service) }.headOption

  def getBookableEvent(id: String): Option[RichEvent] = searchServices(_.getBookableEvent(id))
  def getEvent(id: String): Option[RichEvent] = searchServices(_.getEvent(id))
}
