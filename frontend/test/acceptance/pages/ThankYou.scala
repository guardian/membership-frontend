package acceptance.pages

import acceptance.util.{Config, Browser}
import Config.baseUrl
import org.scalatest.selenium.{WebBrowser, Page}

class ThankYou extends Page with WebBrowser with Browser {
  val url = s"${baseUrl}/join/partner/thankyou"

  def becomePartner = {
    val selector = linkText("Become a Partner")
    assert(pageHasElement(selector))
    click.on(selector)
  }

  def pageHasLoaded(): Boolean = {
    (pageHasElement(id("qa-joiner-summary-tier"))
      && pageHasUrl("join/partner/thankyou"))
  }
}
