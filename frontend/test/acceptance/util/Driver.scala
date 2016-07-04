package acceptance.util

import java.net.URL
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.{Cookie, WebDriver}
import org.openqa.selenium.remote.{RemoteWebDriver, DesiredCapabilities}
import scala.collection.JavaConverters._

/** There should be only a single instance of WebDriver (Singleton Pattern) */
object Driver {
  def apply() = driver
  val sessionId = driver.asInstanceOf[RemoteWebDriver].getSessionId.toString
  def manage() = driver.manage()
  def get(s: String) = driver.get(s)
  def quit() = driver.quit()

  def reset() = {
    Driver.manage().deleteAllCookies()
    Driver.get(Config.baseUrl)
  }

  def cookiesSet: Set[Cookie] = manage().getCookies.asScala.toSet

  def addCookie(name: String, value: String) = manage().addCookie(new Cookie(name, value))

  private lazy val driver: WebDriver =
    if (Config.webDriverRemoteUrl.isEmpty)
      instantiateLocalBrowser()
    else
      instantiateRemoteBrowser()

  private def instantiateLocalBrowser(): WebDriver = {
    val capabilities = DesiredCapabilities.firefox()
    capabilities.setCapability("marionette", true)
    new FirefoxDriver(capabilities)
  }

  private def instantiateRemoteBrowser(): WebDriver = {
    val caps = DesiredCapabilities.internetExplorer()
    caps.setCapability("platform", "Windows 7")
    caps.setCapability("version", "11.0")
    caps.setCapability("name", "membership-frontend")
    new RemoteWebDriver(new URL(Config.webDriverRemoteUrl), caps)
  }
}
