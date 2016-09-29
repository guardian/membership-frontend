package services
import com.gu.memsub.Subscriber.Member
import com.netaporter.uri.dsl._
import configuration.Config
import model.Eventbrite.EBCode
import model.RichEvent.RichEvent
import model.{ContentDestination, ContentItem, Destination, EventDestination}
import scalaz.syntax.monadPlus._
import play.api.mvc.Session
import scala.language.higherKinds
import scalaz.{Monad, OptionT}
import views.support.MembershipCompat._

object DestinationService {
  val JoinReferrer = "join-referrer" //session key
}

class DestinationService[M[+_] : Monad](
  getBookableEvent: String => Option[RichEvent],
  capiItemQuery: String => M[com.gu.contentapi.client.model.v1.ItemResponse],
  createCode: (Member, RichEvent) => M[Option[EBCode]]) {

  /**
    * Given a request from a member, figure out where they came from
    * and get them a destination to go back to (either restricted content or an event)
    */
  def returnDestinationFor(session: Session, member: Member): M[Option[Destination]] = (
    OptionT[M, Destination](contentDestinationFor(session)) orElse
    OptionT[M, Destination](eventDestinationFor(session, member))
  ).run

  /**
    * If the member has come in from some restricted content on The Guardian
    * then lets find the bit of content they want to see and send them back to it
    */
  def contentDestinationFor(session: Session): M[Option[ContentDestination]] = (for {
    guardianReferrer <- OptionT(session.get(DestinationService.JoinReferrer).filter(_.host.contains(Config.guardianHost)).point[M])
    contentItem <- OptionT(capiItemQuery(guardianReferrer.path).map(resp => resp.content.map(ContentItem).map(ContentDestination)))
  } yield contentItem).run

  /**
    * So if the user is joining membership to buy an event we'll have logged the event in the request
    * So we dig it out and if the member is actually now able to book the event we send them back with a discount
    */
  def eventDestinationFor(session: Session, member: Member): M[Option[EventDestination]] = (for {
    eventId <- OptionT(PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(session).point[M])
    event <- OptionT(getBookableEvent(eventId).point[M]) if event.isBookableByTier(member.subscription.plan.tier)
    eventDiscount <- OptionT(createCode(member, event).map[Option[Option[EBCode]]](opt => Some(opt))) // this is perhaps a bit silly
  } yield EventDestination(event, Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> eventDiscount.map(_.code)))).run
}
