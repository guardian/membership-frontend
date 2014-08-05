package services

import play.api.mvc.RequestHeader


object PreMembershipJoiningEventFromSessionExtractor {
  def eventIdFrom(request: RequestHeader): Option[String] = {
    for {
      url <- request.session.get("preJoinReturnUrl")
      parts = url.stripPrefix("/").split('/')
      if parts(0) == "event"
    }  yield parts.takeRight(1).head
  }
}
