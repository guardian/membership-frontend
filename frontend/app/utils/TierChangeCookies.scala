package utils

import configuration.Config
import play.api.mvc.DiscardingCookie

object TierChangeCookies {

  val deletionCookies = List(
      GuMemCookie.deletionCookie,
      DiscardingCookie(
          "gu_paying_member", "/", Some(Config.guardianShortDomain))
  )
}
