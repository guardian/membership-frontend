package model

import configuration.Config

object Nav {

  case class NavItem(
    id: String,
    href: String,
    title: String,
    subNav: Seq[NavItem] = Nil
  )

  def fetchNav(url: String) = {
    val navigationURLs = navigation.find { navItem =>
      // we should return parent item if URL is a subnav
      navItem.href == url || navItem.subNav.map(_.href).contains(url)
    }
    navigationURLs
  }

  val navigation = List(
    NavItem("events", "/events", "Events"),
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("about", "/about", "About membership"),
    NavItem("partrons", "/patrons", "Patrons"),
    NavItem("pricing", "/join", "Pricing"),
    NavItem("feedback", "/feedback", "Feedback")
  )

}
