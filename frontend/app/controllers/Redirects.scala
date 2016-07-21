package controllers

import com.gu.i18n.CountryGroup
import com.netaporter.uri.{PathPart, Uri}
import play.api.mvc._

trait Redirects extends Controller {

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporterRedirect.path)
  }

  def redirectToSupporterPage(countryGroup: CountryGroup): Call = {
    countryGroup match {
      case CountryGroup.UK => routes.Info.supporterUK()
      case CountryGroup.US => routes.Info.supporterUSA()
      case CountryGroup.Europe => routes.Info.supporterEurope()
      case _ => routes.Info.supporterFor(countryGroup)
    }
  }

  def getRedirectCountryCodeGiraffe(countryGroup: CountryGroup): CountryGroup = {
    countryGroup match {
      case CountryGroup.UK => CountryGroup.UK
      case CountryGroup.US => CountryGroup.US
      case CountryGroup.Australia => CountryGroup.Australia
      case CountryGroup.Europe => CountryGroup.Europe
      case CountryGroup.Canada => CountryGroup.Canada
      case _ => CountryGroup.UK
    }
  }

}

object Redirects extends Redirects
