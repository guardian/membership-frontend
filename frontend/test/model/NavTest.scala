package model

import play.api.test.PlaySpecification
import model.Nav.NavItem

class NavTest extends PlaySpecification {

  "Nav" should {
    "find the correct NavItem based on current URL" in {
        Nav.fetchNav("/about").get.id mustEqual "about"
    }

    "return the parent item when the current URL is a subnav item" in {
      Nav.fetchNav("/patrons").get.id mustEqual "about"
    }

  }

}
