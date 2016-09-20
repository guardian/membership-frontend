package services

import play.api.mvc.{RequestHeader, Session}

object EventIdExtractor {
  def apply(url: String): Option[String] = {
    val parts = url.stripPrefix("/").split("/")
    for {
      page <- parts.headOption
      if page == "event"
      eventId <- parts.tail.headOption
    } yield eventId
  }
}

object PreMembershipJoiningEventFromSessionExtractor {
  def eventIdFrom(session: Session): Option[String] = {
    for {
      url <- session.get("preJoinReturnUrl")
      eventId <- EventIdExtractor(url)
    }  yield eventId
  }
}
