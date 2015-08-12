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
    NavItem("masterclasses", "/masterclasses", "Masterclasses"),
    NavItem("competitions", "/offers-competitions", "Competitions"),
    NavItem("patrons", "/patrons", "Patrons"),
    NavItem("feedback", "/feedback", "Feedback")
  )

}
