package model

import com.gu.i18n.CountryGroup
import configuration.Links
import controllers.routes

object Nav {

  case class NavItem(
    id: String,
    href: String,
    title: String,
    isPrivate: Boolean = false,
    subNav: Seq[NavItem] = Nil
  )

  def fetchNav(navigationList: Seq[NavItem], url: String) = {
    navigationList.find(navItem => url.contains(navItem.href))
  }

  val primaryNavigation = List(
    NavItem("events", routes.WhatsOn.list().url, "Events", subNav = Seq(
      NavItem("archive", routes.WhatsOn.listArchive.url, "Archive")
    )),
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("support the guardian", "https://support.theguardian.com/?acquisitionData=%7B%22source%22%3A%22GUARDIAN_WEB%22%2C%22componentId%22%3A%22become_a_supporter_link_on_membership_site%22%7D", "Support the Guardian"),
    NavItem("patrons", Links.patrons, "Patrons")
  )

  val internationalLandingPageNavigation = List()

  def footerNavigation(countryGroup: Option[CountryGroup] = None) = List(
    NavItem("masterclassesFAQ", Links.guardianMasterclassesFAQs, "Masterclasses FAQ"),
    NavItem("liveFAQ", Links.guardianLiveFAQs, "GuardianLive FAQ"),
    NavItem("contact", Links.membershipContact, "Contact us"),
    NavItem("terms", Links.membershipTerms(countryGroup), "Terms & conditions"),
    NavItem("privacy", Links.guardianPrivacyPolicy, "Privacy policy"),
    NavItem("cookies", Links.guardianCookiePolicy, "Cookie policy")
  )

}
