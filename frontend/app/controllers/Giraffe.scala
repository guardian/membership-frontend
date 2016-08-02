package controllers

import com.gu.i18n._
import play.api.mvc._

object Giraffe extends Controller {

  def redirectToContributions() = CachedAction { implicit request =>
    MovedPermanently("https://contribute.theguardian.com/")
  }

  def redirectToContributionsFor(countryGroup: CountryGroup) = CachedAction { implicit request =>
    MovedPermanently(s"https://contribute.theguardian.com/${countryGroup.id}")
  }

}
