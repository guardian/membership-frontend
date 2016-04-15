package acceptance.pages

import acceptance.util.{Config, Browser}
import org.scalatest.selenium.Page

object ThankYou extends Page with Browser {
  val url = s"${Config.baseUrl}/join/partner/thankyou"

  def userIsSignedInAsPartner: Boolean = elementHasText(cssSelector(".js-user-tier"), "Partner")

  def pageHasLoaded: Boolean = (pageHasElement(id("qa-joiner-summary-tier")) && pageHasUrl("join/partner/thankyou"))
}
