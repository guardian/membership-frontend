package acceptance.util

import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.openqa.selenium.{By, Cookie}
import org.scalatest.selenium.WebBrowser
import scala.util.Try
import scala.collection.JavaConverters._

trait WebBrowserUtil { this: WebBrowser =>

  lazy implicit val driver = Config.driver

  protected def resetDriver() = {
    driver.get("about:blank")
    go.to(Config.baseUrl)
    driver.manage().deleteAllCookies()
  }

  protected def pageHasText(text: String): Boolean = {
    waitUntil(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text))
  }

  protected def pageHasElement(q: Query): Boolean = {
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))
  }

  protected def pageHasUrl(urlFraction: String): Boolean = {
    waitUntil(ExpectedConditions.urlContains(urlFraction))
  }

  protected def cookiesSet: Set[Cookie] = driver.manage().getCookies.asScala.toSet

  private def waitUntil[T](pred: ExpectedCondition[T]) = {
    Try(new WebDriverWait(driver, timeOutSec).until(pred)).isSuccess
  }

  private val timeOutSec = 60
}
