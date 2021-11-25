package services.api

import _root_.services.EventbriteCollectiveServices
import com.gu.memsub.Subscriber._
import model.Eventbrite.EBCode
import model.RichEvent.RichEvent

import scala.concurrent.Future

trait MemberService {

  type ZuoraSubName = String

  def createEBCode(subscriber: Member, event: RichEvent)(implicit services: EventbriteCollectiveServices): Future[Option[EBCode]]

}
