package acceptance.pages

import acceptance.util.{Config, Browser}
import org.scalatest.selenium.Page

object ThankYou extends Page with Browser {
  val url = s"${Config.baseUrl}/join/supporter/thankyou"

  def userIsSignedInAsSupporter: Boolean = elementHasText(cssSelector(".js-user-tier"), "Supporter")

  def pageHasLoaded: Boolean = (pageHasElement(id("qa-joiner-summary-tier")) && pageHasUrl("join/supporter/thankyou"))
}
