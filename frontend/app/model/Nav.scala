package model

import configuration.Config

object Nav {

  case class NavItem(
    id: String,
    href: String,
    title: String,
    subNav: Seq[NavItem] = Nil,
    footerOnly: Boolean = false
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
    NavItem("about", "/about", "About membership", Seq(
        NavItem("supporters", "/supporters", "Supporters"),
        NavItem("partners", "/partners", "Partners"),
        NavItem("patrons", "/patrons", "Patrons")
      )
    ),
    NavItem("pricing", "/join", "Pricing"),
    NavItem("help", "/help", "Help", Nil, true),
    NavItem("feedback", "/feedback", "Feedback", Nil, true)
  )

}
