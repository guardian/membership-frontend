package filters


import com.netaporter.uri.Uri
import configuration.Config
import play.api.http.Status.FOUND
import play.api.mvc.Results.Redirect
import play.api.mvc._

import scala.concurrent.Future

object RedirectMembersFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (requestHeader.host.toLowerCase.startsWith("members.")) {
      val requestUri = Uri.parse(requestHeader.uri)
      Future.successful(Redirect(requestUri.withScheme("https").withHost(Config.membershipHost).toString, FOUND))
    } else nextFilter(requestHeader)
  }
}
