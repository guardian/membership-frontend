package controllers

import com.gu.i18n.CountryGroup
import com.netaporter.uri.{PathPart, Uri}
import play.api.mvc._

trait Redirects extends Controller {

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporterRedirect(None).path)
  }

  def redirectToSupporterPage(countryGroup: CountryGroup): Call = {
    countryGroup match {
      case CountryGroup.US => routes.Info.supporterUSA()
      case _ => routes.Info.supporterFor(countryGroup)
    }
  }

}

object Redirects extends Redirects
