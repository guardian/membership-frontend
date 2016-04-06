package services

import play.api.mvc.RequestHeader

object EventIdExtractor {
  def apply(url: String): Option[String] = {
    val parts = url.stripPrefix("/").split("/")
    for {
      page <- parts.headOption if page == "event"
      eventId <- parts.tail.headOption
    } yield eventId
  }
}

object PreMembershipJoiningEventFromSessionExtractor {
  def eventIdFrom(request: RequestHeader): Option[String] = {
    for {
      url <- request.session.get("preJoinReturnUrl")
      eventId <- EventIdExtractor(url)
    } yield eventId
  }
}
