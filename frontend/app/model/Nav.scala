package model

import configuration.Links

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
    NavItem("events", "/events", "Events"),
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("competitions", "/offers-competitions", "Competitions"),
    NavItem("patrons", "/patrons", "Patrons"),
    NavItem("feedback", "/feedback", "Feedback")
  )

  val footerNavigation = List(
    NavItem("help", controllers.Info.help.toString, "Help"),
    NavItem("contact", Links.membershipContact, "Contact us"),
    NavItem("feedback", controllers.Info.feedback.toString, "Feedback"),
    NavItem("terms", Links.membershipTerms, "Terms & conditions"),
    NavItem("privacy", Links.guardianPrivacyPolicy, "Privacy policy"),
    NavItem("cookies", Links.guardianCookiePolicy, "Cookie policy")
  )

}
