package model

import play.api.test.PlaySpecification

class NavTest extends PlaySpecification {

  "Nav" should {
    "find the correct NavItem based on current URL" in {
      Nav.fetchNav("/about").get.id mustEqual "about"
    }

    "return the parent item when the current URL is a subnav item" in {
      Nav.fetchNav("/whats-on").get.id mustEqual "whats-on"
      Nav.fetchNav("/whats-on/calendar").get.id mustEqual "whats-on"
      Nav.fetchNav("/not-whats-on") mustEqual None
    }

  }

}
