package acceptance.util

import java.net.URL
import java.util.concurrent.TimeUnit

import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, Cookie}
import org.scalatest.selenium.WebBrowser

import scala.collection.JavaConverters._
import scala.util.Try

trait Util { this: WebBrowser =>

  lazy implicit val driver = Config.driver

  def resetDriver() = {
    driver.get("about:blank")
    go.to(Config.baseUrl)
    driver.manage().deleteAllCookies()
    driver.manage().timeouts().implicitlyWait(defaultTimeOut, TimeUnit.SECONDS)
  }

  private val defaultTimeOut = 90

  protected def pageHasText(text: String, timeoutSecs: Int=defaultTimeOut): Boolean = {
    val pred = ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text)
    Try {
      new WebDriverWait(driver, timeoutSecs).until(pred)
    }.isSuccess
  }

  protected def pageHasElement(q: Query, timeoutSecs: Int=defaultTimeOut): Boolean = {
    val pred = ExpectedConditions.visibilityOfElementLocated(q.by)
    Try {
      new WebDriverWait(driver, timeoutSecs).until(pred)
    }.isSuccess
  }

  protected def currentHost: String = new URL(currentUrl).getHost

  def cookiesSet: Set[Cookie] = driver.manage().getCookies.asScala.toSet
}
