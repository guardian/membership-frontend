package acceptance.pages

import acceptance.util.{Config, Browser}
import org.scalatest.selenium.Page

object ThankYou extends Page with Browser {
  val url = s"${Config.baseUrl}/join/supporter/thankyou"

  def userIsSignedInAsSupporter: Boolean = elementHasText(cssSelector(".js-user-tier"), "Supporter")

  def pageHasLoaded: Boolean = {
    // ensure that we are looking at the main page, and not the Stripe/Paypal iframe that may have just closed
    driver.switchTo().defaultContent()

    pageHasElement(id("qa-joiner-summary-tier")) && pageHasUrl("join/supporter/thankyou")
  }
}
