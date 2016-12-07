package filters


import com.netaporter.uri.Uri
import configuration.Config
import play.api.http.Status.FOUND
import play.api.mvc.Results.Redirect
import play.api.mvc._
import tracking.RedirectWithCampaignCodes.internalCampaignCode

import scala.concurrent.Future

object RedirectMembersFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    if (requestHeader.host.toLowerCase.startsWith("members.")) {
      val requestUri = Uri.parse(requestHeader.uri)
      val redirectUri = requestUri.query.param(internalCampaignCode) match {
        case None => requestUri.addParam(internalCampaignCode, "MEMBERS_DOMAIN_REDIRECT")
        case _ => requestUri
      }
      Future.successful(Redirect(redirectUri.withScheme("https").withHost(Config.membershipHost).toString, FOUND))
    } else nextFilter(requestHeader)
  }
}
