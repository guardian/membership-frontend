package controllers

import actions.CommonActions
import configuration.Links
import play.api.mvc._

class Redirects(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CachedAction

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporterRedirect(None).path)
  }

  def supportRedirect = CachedAction(MovedPermanently("https://support.theguardian.com/"))
}
