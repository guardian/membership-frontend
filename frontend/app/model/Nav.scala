package model

object Nav {

  case class NavItem(
    id: String,
    href: String,
    title: String,
    isPrivate: Boolean = false,
    subNav: Seq[NavItem] = Nil
  )

  def fetchNav(url: String) = {
    val navigationURLs = navigation.find { navItem =>
      // Return the parent item if URL is a subnav
      navItem.href == url || navItem.subNav.map(_.href).contains(url)
    }
    navigationURLs
  }

  val navigation = List(
    NavItem("events", "/events", "Events"),
    NavItem("whats-on", "/whats-on", "What's On", isPrivate = true,
      subNav = List(
        NavItem("calendar", "/whats-on/calendar", "Calendar")
      )
    ),
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("offers", "/offers-competitions", "Offers"),
    NavItem("about", "/about", "About membership"),
    NavItem("partrons", "/patrons", "Patrons"),
    NavItem("pricing", "/join", "Pricing"),
    NavItem("feedback", "/feedback", "Feedback")
  )

}
