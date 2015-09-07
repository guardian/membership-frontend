package configuration

import controllers.routes

object Links {
  val guardianCookiePolicy = "http://www.theguardian.com/info/cookies"
  val guardianPrivacyPolicy = "http://www.theguardian.com/help/privacy-policy"

  val guardianLiveTerms = "http://www.theguardian.com/info/2014/sep/09/guardian-live-events-terms-and-conditions"
  val guardianMasterclassesTerms = "http://www.theguardian.com/guardian-masterclasses/terms-and-conditions"

  val membershipFront = "http://www.theguardian.com/membership"

  val membershipTerms = "http://www.theguardian.com/info/2014/sep/09/guardian-membership-terms-and-conditions"
  val membershipContact= "http://www.theguardian.com/help/contact-us#Membership"

  val membershipBuilding = "http://www.theguardian.com/membership/midland-goods-shed-progress/2014/sep/10/-sp-midland-goods-shed-guardian-events-membership-building-space"
  val membershipBuildingBlog = "http://theguardian.com/membership/midland-goods-shed-progress/"
  val membershipPollyToynbeeArticle = "http://www.theguardian.com/membership/2015/feb/06/polly-toynbee-if-you-read-the-guardian-join-the-guardian"
}

object ProfileLinks {

  val commentActivity = s"${Config.idWebAppUrl}/user/id/"

  val editProfile = s"${Config.idWebAppUrl}/public/edit"

  val editProfileMembership = s"${Config.idWebAppUrl}/membership/edit"

  val emailPreferences = s"${Config.idWebAppUrl}/email-prefs"

  val changePassword = s"${Config.idWebAppUrl}/password/change"

  def signOut(path: String) = {

    val baseUrl = s"${Config.idWebAppUrl}/signout"
    val exclusions = Seq(
      routes.FrontPage.welcome.url
    )

    if(exclusions.contains(path)) {
      s"$baseUrl?returnUrl=${Config.membershipUrl}"
    } else baseUrl

  }

}

