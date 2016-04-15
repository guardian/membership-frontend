package acceptance.util

import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.openqa.selenium.By
import org.scalatest.selenium.WebBrowser
import scala.util.Try

trait Browser extends WebBrowser {

  lazy implicit val driver = Driver()

  def pageHasText(text: String): Boolean =
    waitUntil(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text))

  def pageHasElement(q: Query): Boolean =
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))

  def pageHasUrl(urlFraction: String): Boolean =
    waitUntil(ExpectedConditions.urlContains(urlFraction))

  def elementHasText(q: Query, text: String): Boolean =
    waitUntil(ExpectedConditions.textToBePresentInElementLocated(q.by, text))

  def elementHasValue(q: Query, text: String): Boolean =
    waitUntil(ExpectedConditions.textToBePresentInElementValue(q.by, text))

  def clickOn(q: Query) {
    if (pageHasElement(q))
      click.on(q)
    else
      throw new MissingPageElementException(q)
  }

  def setValue(q: Query, value: String) {
    if (pageHasElement(q))
      q.webElement.sendKeys(value)
    else
      throw new MissingPageElementException(q)
  }

  def setRadioButtonValue(q: NameQuery, value: String) {
    if (pageHasElement(q))
      radioButtonGroup(q.queryString).value = value
    else
      throw new MissingPageElementException(q)
  }

  def setSingleSelectionValue(q: Query, value: String) {
    if (pageHasElement(q))
      singleSel(q).value = value
    else
      throw new MissingPageElementException(q)
  }

  private def waitUntil[T](pred: ExpectedCondition[T]): Boolean =
    Try(new WebDriverWait(driver, Config.waitTimout).until(pred)).isSuccess

  private case class MissingPageElementException(q: Query)
    extends Exception(s"Could not find WebElement with locator: ${q.queryString}")
}
