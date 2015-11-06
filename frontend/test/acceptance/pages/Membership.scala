package acceptance.pages

import acceptance.Config.baseUrl
import acceptance.Util
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}

class Membership(implicit val driver: WebDriver) extends Page with WebBrowser with Util {
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
