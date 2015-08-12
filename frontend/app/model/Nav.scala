package model

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

}
