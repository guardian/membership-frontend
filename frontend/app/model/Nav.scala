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
    NavItem("events", routes.WhatsOn.list.url, "Events", subNav = Seq(
      NavItem("archive", routes.WhatsOn.listArchive.url, "Archive")
    )),
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("patrons", "/patrons", "Patrons"),
    NavItem("feedback", "/feedback", "Feedback")
  )

  val internationalLandingPageNavigation = List(
    NavItem("feedback", "/feedback", "Feedback")
  )

  def footerNavigation(countryGroup: Option[CountryGroup] = None) = List(
    NavItem("help", routes.Info.help().toString, "Help"),
    NavItem("contact", Links.membershipContact, "Contact us"),
    NavItem("feedback", routes.Info.feedback().toString, "Feedback"),
    NavItem("terms", Links.membershipTerms(countryGroup), "Terms & conditions"),
    NavItem("privacy", Links.guardianPrivacyPolicy, "Privacy policy"),
    NavItem("cookies", Links.guardianCookiePolicy, "Cookie policy")
  )

}
