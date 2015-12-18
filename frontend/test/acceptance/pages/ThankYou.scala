package acceptance.pages

import acceptance.util.{Config, WebBrowserUtil}
import Config.baseUrl
import org.scalatest.selenium.{WebBrowser, Page}

class ThankYou extends Page with WebBrowser with WebBrowserUtil {
  val url = s"${baseUrl}/join/partner/thankyou"

  def userDisplayName: String = {
    val selector = cssSelector(".js-user-displayname")
    assert(pageHasElement(selector))
    selector.element.text
  }

  def userTier: String = {
    val selector = cssSelector(".js-user-tier")
    assert(pageHasElement(selector))
    selector.element.text
  }

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
