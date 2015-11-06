package acceptance.pages

import acceptance.Config.baseUrl
import acceptance.{TestUser, Util}
import org.openqa.selenium.{WebElement, By, WebDriver}
import org.scalatest.selenium.{WebBrowser, Page}

class EnterDetails(implicit val driver: WebDriver) extends Page with WebBrowser with Util {
  val url = s"$baseUrl/join/partner/enter-details"

  def userDisplayName: String = {
    val selector = cssSelector(".js-user-displayname")
    assert(pageHasElement(selector))
    selector.element.text
  }

  def becomePartner = {
    val selector = cssSelector("a[href='/join/partner/enter-details']")
    assert(pageHasElement(selector))
    click on selector
  }

  def pageHasLoaded(): Boolean = {
    pageHasElement(id("cc-cvc"))
  }

  def fillInDeliveryAddress() = {
    DeliveryAddress.fillIn()
  }

  def fillInCardDetails() = {
    CardDetails.fillIn()
  }

  def pay() = {
    val selector = className("js-submit-input")
    assert(pageHasElement(selector))

    assert(TestUser.isValid(Name.first.value), "Invalid test-user!")

    click.on(selector)
  }

  private object Name {
    val first = textField(id("name-first"))
    val last = textField(id("name-last"))
  }

  private object DeliveryAddress {
    val addressLine1 = textField(id("address-line-one-deliveryAddress"))
    val town = textField(id("town-deliveryAddress"))
    val postCode = textField(id("postCode-deliveryAddress"))

    def fillIn(): Unit = {
      assert(pageHasElement(id("postCode-deliveryAddress")))

      addressLine1.value = "Kings Place"
      town.value = "London"
      postCode.value = "N1 9GU"
    }
  }

  private object CardDetails {
    val cardNumber = textField(id("cc-num"))
    val securityCode = textField(id("cc-cvc"))
    val expiryMonth = singleSel(id("cc-exp-month"))
    val expiryYear = singleSel(id("cc-exp-year"))

    def fillIn(): Unit = {
      assert(pageHasElement(id("cc-exp-year")))
      assert(pageHasElement(id("cc-num")))
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4242424242424242"
      securityCode.value = "123"
      expiryMonth.value = "1"
      expiryYear.value = "2017"
    }
  }
}
