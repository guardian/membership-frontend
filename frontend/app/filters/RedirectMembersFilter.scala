package filters


import configuration.Config
import play.api.mvc._

import scala.concurrent.Future

object RedirectMembersFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val host: String = requestHeader.host.toLowerCase()
    if (host.contains("members.")) {
      Future.successful(Results.Redirect(s"${Config.membershipUrl}${requestHeader.uri}", 302));
    }
    else {
      nextFilter(requestHeader);
    }
  }
}
