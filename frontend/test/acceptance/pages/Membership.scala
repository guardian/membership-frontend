package acceptance.pages

import acceptance.util.{Config, Util}
import Config.baseUrl
import acceptance.util.Util
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}

class Membership extends Page with WebBrowser with Util {
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
      && currentUrl.startsWith(url))
  }
}
