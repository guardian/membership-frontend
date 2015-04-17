package services

import actions._
import configuration.Config
import model.{Destination, EventDestination, MembersOnlyContent, ContentDestination}
import com.netaporter.uri.dsl._
import play.api.mvc.Session
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext


trait DestinationService {

  val JoinReferrer = "join-referrer"
  val contentApiService: GuardianContentService

  def returnDestinationFor(request: AnyMemberTierRequest[_]): Future[Option[Destination]] = {
    Future.sequence(Seq(contentDestinationFor(request), eventDestinationFor(request))).map(_.flatten.headOption)
  }

  def contentDestinationFor(request: AnyMemberTierRequest[_]): Future[Option[ContentDestination]] = {
    request.session.get(JoinReferrer).map { referer =>
      if(referer.host.contains(Config.guardianHost)) {
        contentApiService.contentItemQuery(referer.path).map { resp =>
          resp.content.map(MembersOnlyContent).map(ContentDestination(_))
        } recover { case _ => None }
      } else {
        Future.successful(None)
      }
    }.getOrElse(Future.successful(None))
  }

  def eventDestinationFor(request: AnyMemberTierRequest[_]): Future[Option[EventDestination]] = {
    val optFuture = for {
      eventId <- PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
      event <- EventbriteService.getBookableEvent(eventId)
    } yield MemberService.createDiscountForMember(request.member, event).map { discountOpt =>
        EventDestination(event, (Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code))))
      }

    Future.sequence(optFuture.toSeq).map(_.headOption)
  }
}

object DestinationService extends DestinationService {
  val contentApiService = GuardianContentService
}

