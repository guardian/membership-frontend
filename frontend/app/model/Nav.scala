package model

object Nav {

  case class NavItem(
    id: String,
    href: String,
    title: String,
    isPrivate: Boolean = false,
    subNav: Seq[NavItem] = Nil
  )

  case class NavAnchor(href: String, title: String)

  def fetchNav(url: String) =
    navigation.find(navItem => url.contains(navItem.href))

  val navigation = List(
    NavItem("events", "/events", "Events"),
    NavItem("whats-on", "/whats-on", "What's On", isPrivate = true,
      subNav = List(
        NavItem("calendar", "/whats-on/calendar", "Calendar")
      )
    ),
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("competitions", "/offers-competitions", "Competitions"),
    NavItem("about", "/about", "About membership"),
    NavItem("partrons", "/patrons", "Patrons"),
    NavItem("pricing", "/join", "Pricing"),
    NavItem("feedback", "/feedback", "Feedback")
  )

}
