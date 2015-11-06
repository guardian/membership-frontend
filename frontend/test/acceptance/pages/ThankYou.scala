package acceptance.pages

import acceptance.Config.baseUrl
import acceptance.Util
import org.openqa.selenium.WebDriver
import org.scalatest.selenium.{WebBrowser, Page}

class ThankYou(implicit val driver: WebDriver) extends Page with WebBrowser with Util {
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
    (pageHasText("Welcome to Guardian Members")
      && currentUrl.endsWith("join/partner/thankyou"))
  }
}
