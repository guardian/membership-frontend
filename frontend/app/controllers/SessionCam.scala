package controllers

import configuration.Config.sessionCamCookieName
import play.api.mvc.{DiscardingCookie, Cookie, Controller}

object SessionCam extends Controller {
    // Temporary solution to evaluate SessionCam without actually activating it across the entire site
  def dropSessionCamCookie = NoCacheAction { implicit request =>
    Redirect(routes.FrontPage.welcome()).withCookies(
      Cookie(name = sessionCamCookieName, value = "y", httpOnly = false))
  }

  def removeSessionCamCookie = NoCacheAction { implicit request =>
    Redirect(routes.FrontPage.welcome()).discardingCookies(DiscardingCookie(sessionCamCookieName))
  }
}
