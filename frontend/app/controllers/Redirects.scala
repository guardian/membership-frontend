package controllers

import actions.CommonActions
import com.gu.i18n.CountryGroup
import configuration.Links
import play.api.mvc._

class Redirects(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CachedAction

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporterRedirect(None).path)
  }

  def supportRedirect = CachedAction{ implicit request =>
    Redirect("https://support.theguardian.com/", request.queryString, MOVED_PERMANENTLY)
  }

  def whySupportRedirectIgnore(ignore: String) = CachedAction{ implicit request =>
    Redirect("https://support.theguardian.com/support", request.queryString, MOVED_PERMANENTLY)
  }

  def whySupportRedirect() = whySupportRedirectIgnore("")

}
