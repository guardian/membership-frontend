package controllers

import play.api.mvc.Controller
import com.netaporter.uri.dsl._

trait VanityUrl extends Controller {

  def redirect = CachedAction { implicit request =>
    Redirect(routes.FrontPage.index().url ? ("INTCMP" -> "pap_233874"))
  }
}

object VanityUrl extends VanityUrl
