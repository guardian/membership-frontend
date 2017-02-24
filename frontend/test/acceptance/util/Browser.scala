package acceptance.util

import org.openqa.selenium.support.ui.{ExpectedCondition, ExpectedConditions, WebDriverWait}
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions.numberOfWindowsToBe
import org.scalatest.selenium.WebBrowser

import scala.util.Try
import scala.collection.JavaConverters.asScalaSetConverter

trait Browser extends WebBrowser {

  lazy implicit val driver = Driver()
  // Stores a handle to the first window opened by the driver.
  lazy val parentWindow = driver.getWindowHandle

  def pageHasText(text: String): Boolean =
    waitUntil(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text))

  def pageHasElement(q: Query): Boolean =
    waitUntil(ExpectedConditions.visibilityOfElementLocated(q.by))

  def pageHasUrl(urlFraction: String): Boolean =
    waitUntil(ExpectedConditions.urlContains(urlFraction))

  def elementHasText(q: Query, text: String): Boolean =
    waitUntil(ExpectedConditions.textToBePresentInElementLocated(q.by, text))

  def hiddenElementHasText(q: Query, text: String): Boolean =
    waitUntil(ExpectedConditions.attributeContains(q.by, "textContent", text))

  def elementHasValue(q: Query, text: String): Boolean =
    waitUntil(ExpectedConditions.textToBePresentInElementValue(q.by, text))

  def clickOn(q: Query) {
    if (pageHasElement(q))
      click.on(q)
    else
      throw new MissingPageElementException(q)
  }

  def setValue(q: Query, value: String, clear: Boolean = false) {
    if (pageHasElement(q)) {

      if (clear) q.webElement.clear
      q.webElement.sendKeys(value)

    } else
      throw new MissingPageElementException(q)
  }

  /*
    * Stripe wants you to pause between month and year and between each quartet in the cc
    * This causes pain when you use Selenium. There are a few stack overflow posts- but nothing really useful.
    * This pausing also seems to be necessary to make PayPal work properly,
    * without this it starts to complain about invalid credentials after only
    * a few characters.
    * */
  def setValueSlowly(q: Query, value: String): Unit = {
    for {
      c <- value
    } yield {
      setValue(q, c.toString)
      Thread.sleep(100)
    }
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

  // Switches to a new iframe specified by the Query, q.
  def switchFrame(q: Query) {
    if (pageHasElement(q))
      driver.switchTo().frame(q.webElement)
    else
      throw new MissingPageElementException(q)
  }

  /*
   * Switches to the first window in the list of windows that doesn't match
   * the parent window.
   * */
  def switchWindow() {
    waitUntil(numberOfWindowsToBe(2))

    for {
      winHandle <- driver.getWindowHandles.asScala
      if winHandle != parentWindow
    } driver.switchTo().window(winHandle)

  }

  // Switches back to the first window opened by the driver.
  def switchToParentWindow() = driver.switchTo().window(parentWindow)

  def changeUrl(url: String) = driver.get(url)

  private def waitUntil[T](pred: ExpectedCondition[T]): Boolean =
    Try(new WebDriverWait(driver, Config.waitTimeout).until(pred)).isSuccess

  private case class MissingPageElementException(q: Query)
    extends Exception(s"Could not find WebElement with locator: ${q.queryString}")
}
