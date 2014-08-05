package services

import play.api.mvc.RequestHeader


object PreMembershipJoiningEventFromSessionExtractor {
  def eventIdFrom(request: RequestHeader): Option[String] = {
    request.session.get("preJoinReturnUrl").map(_.split('/').takeRight(2).head)
  }
}
