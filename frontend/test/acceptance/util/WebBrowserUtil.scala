package acceptance.util

import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.openqa.selenium.By
import org.scalatest.selenium.WebBrowser
import scala.util.Try


trait WebBrowserUtil { this: WebBrowser =>
  lazy implicit val driver = Driver()

  protected def pageHasText(text: String): Boolean = {
    waitUntil(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text))
  }

  protected def pageHasElement(q: Query): Boolean = {
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))
  }

  protected def pageHasUrl(urlFraction: String): Boolean = {
    waitUntil(ExpectedConditions.urlContains(urlFraction))
  }

  private def waitUntil[T](pred: ExpectedCondition[T]) = {
    Try(new WebDriverWait(driver, timeOutSec).until(pred)).isSuccess
  }

  private val timeOutSec = 30
}
