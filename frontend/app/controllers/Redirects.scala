package controllers

import com.gu.i18n.CountryGroup
import play.api.mvc.{Result, Call, Controller}

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

  def redirectToGiraffe(countryGroup: CountryGroup): Call = {
    countryGroup match {
      case CountryGroup.UK => routes.Giraffe.contributeUK()
      case CountryGroup.US => routes.Giraffe.contributeUSA()
      case CountryGroup.Australia => routes.Giraffe.contributeAustralia()

      case _ => routes.Giraffe.contributeUSA()
    }
  }
}

object Redirects extends Redirects
