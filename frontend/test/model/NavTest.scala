package model

import model.Nav.NavItem
import play.api.test.PlaySpecification

class NavTest extends PlaySpecification {

  val testNav = List(
    NavItem("page-a", "/page-a", "Page A"),
    NavItem("page-b", "/page-b", "Page B",
      subNav = List(
        NavItem("sub-page", "/page-b/sub-page", "Sub Page")
      )
    ),
    NavItem("page-c", "/page-c", "Page C")
  )

  "Nav" should {
    "find the correct NavItem based on current URL" in {
      Nav.fetchNav(testNav, "/page-a").get.id mustEqual "page-a"
    }

    "return the parent item when the current URL is a subnav item" in {
      Nav.fetchNav(testNav, "/page-b").get.id mustEqual "page-b"
      Nav.fetchNav(testNav, "/page-b/sub-page").get.id mustEqual "page-b"
      Nav.fetchNav(testNav, "/not-a-page") mustEqual None
    }

  }

}
