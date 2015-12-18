package acceptance.pages

import acceptance.util.{WebBrowserUtil, Config}
import Config.baseUrl
import org.scalatest.selenium.{WebBrowser, Page}

class Membership extends Page with WebBrowser with WebBrowserUtil {
  val url = baseUrl

  def userDisplayName: String = {
    val selector = cssSelector(".js-user-displayname")
    assert(pageHasElement(selector))
    selector.element.text
  }

  def becomePartner = {
    val selector = linkText("Become a Partner")
    assert(pageHasElement(selector))
    click.on(selector)
  }

  def pageHasLoaded(): Boolean = {
    (pageHasElement(cssSelector("a[href='/join/partner/enter-details']"))
      && pageHasUrl(url))
  }
}
