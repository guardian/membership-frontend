package services

import actions._
import com.netaporter.uri.dsl._
import configuration.Config
import model.{ContentDestination, ContentItem, Destination, EventDestination}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

trait DestinationService {

  def memberService(request: SubscriptionRequest[_]): api.MemberService

  val JoinReferrer = "join-referrer"
  val contentApiService: GuardianContentService
  val eventbriteService: EventbriteCollectiveServices

  def returnDestinationFor(request: SubscriptionRequest[_] with Subscriber): Future[Option[Destination]] = {
    Future.sequence(Seq(contentDestinationFor(request), eventDestinationFor(request))).map(_.flatten.headOption)
  }

  def contentDestinationFor(implicit request: SubscriptionRequest[_]): Future[Option[ContentDestination]] = {
    request.session.get(JoinReferrer).map { referer =>
      if(referer.host.contains(Config.guardianHost)) {
        contentApiService.contentItemQuery(referer.path).map { resp =>
          resp.content.map(ContentItem).map(ContentDestination)
        } recover { case _ => None }
      } else {
        Future.successful(None)
      }
    }.getOrElse(Future.successful(None))
  }

  def eventDestinationFor(implicit request: SubscriptionRequest[_] with Subscriber): Future[Option[EventDestination]] = {
    val optFuture = for {
      eventId <- PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
      event <- eventbriteService.getBookableEvent(eventId)
    } yield memberService(request).createEBCode(request.subscriber, event).map { discountOpt =>
      EventDestination(event, Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code)))
    }

    optFuture.map(_.map(Some(_))).getOrElse(Future.successful(None))
  }
}

object DestinationService extends DestinationService  {
  val contentApiService = GuardianContentService
  val eventbriteService = EventbriteService

  override def memberService(request: SubscriptionRequest[_]): api.MemberService =
    request.touchpointBackend.memberService
}
