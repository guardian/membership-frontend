package services

import scala.concurrent._
import model.MembershipEvent
import scala.concurrent.ExecutionContext.Implicits.global


trait EventService {
  def getEvents(): Future[Seq[MembershipEvent]]
}

class EventBriteService extends EventService {
  override def getEvents(): Future[Seq[MembershipEvent]] = future{Nil}
}


