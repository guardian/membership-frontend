package configuration

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup.{UK,US,Australia}
import controllers.routes
import io.lemonlabs.uri.typesafe.dsl._

object Links {
  val guardianCookiePolicy = "http://www.theguardian.com/info/cookies"
  val guardianPrivacyPolicy = "http://www.theguardian.com/help/privacy-policy"

  val guardianLiveTerms = "http://www.theguardian.com/info/2014/sep/09/guardian-live-events-terms-and-conditions"
  val guardianMasterclassesTerms = "http://www.theguardian.com/guardian-masterclasses/terms-and-conditions"

  val guardianLiveFAQs = "https://www.theguardian.com/guardian-masterclasses/2020/aug/27/guardian-live-online-faqs"
  val guardianMasterclassesFAQs = "https://www.theguardian.com/guardian-masterclasses/2020/jul/20/guardian-masterclasses-faqs"

  val membershipFront = "http://www.theguardian.com/membership"

  def membershipTerms(countryGroup: Option[CountryGroup] = None) = {
    countryGroup match {
      case Some(UK) => "https://www.theguardian.com/info/2014/sep/09/guardian-membership-terms-and-conditions"
      case Some(US) => "https://www.theguardian.com/info/2016/nov/08/guardian-members-us-terms-and-conditions"
      case Some(Australia)  => "https://www.theguardian.com/info/2016/nov/08/guardian-members-australia-terms-and-conditions"
      case _ => "https://www.theguardian.com/info/2016/nov/08/guardian-members-international-terms-and-conditions"
    }
  }

  val monthlyContributionTerms = "https://www.theguardian.com/info/2016/apr/04/contribution-terms-and-conditions"

  val membershipContact= "http://www.theguardian.com/help/contact-us#Membership"

  val membershipPollyToynbeeArticle = "http://www.theguardian.com/membership/2015/feb/06/polly-toynbee-if-you-read-the-guardian-join-the-guardian"

  val giraffeTerms = "https://www.theguardian.com/info/2016/apr/04/contribution-terms-and-conditions"
  val giraffeTermsUS = "https://www.theguardian.com/info/2016/apr/07/us-contribution-terms-and-conditions"
  val giraffeTermsAustralia = "https://www.theguardian.com/info/2016/apr/08/australia-contribution-terms-and-conditions"

  val patrons = "https://patrons.theguardian.com/"
}

object ProfileLinks {

  val commentActivity =  Config.idWebAppUrl /  "user/id/"

  val editProfile =  Config.idWebAppUrl / "public/edit" ? Config.idMember

  val editProfileMembership =  Config.idWebAppUrl / "membership/edit" ? Config.idMember

  val emailPreferences =  Config.idWebAppUrl / "email-prefs" ? Config.idMember

  val changePassword =  Config.idWebAppUrl / "reset" ? Config.idMember

  def signOut(path: String) = {
    Config.idWebAppUrl / "signout" ? Config.idMember ? ("returnUrl" -> (Config.membershipUrl + path))
  }

}

