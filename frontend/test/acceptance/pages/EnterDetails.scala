package acceptance.pages

import acceptance.util.{Util, Config}
import Config.baseUrl
import org.scalatest.selenium.{WebBrowser, Page}

class EnterDetails extends Page with WebBrowser with Util {
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
    CreditCard.fillIn()
  }

  def fillInCardDeclined(): Unit = {
    CreditCard.fillInCardDeclined()
  }

  def fillInCardDeclinedFraud(): Unit = {
    CreditCard.fillInCardDeclinedFraud()
  }

  def fillInCardDeclinedCvc(): Unit = {
    CreditCard.fillInCardDeclinedCvc()
  }

  def fillInCardDeclinedExpired(): Unit = {
    CreditCard.fillInCardDeclinedExpired()
  }

  def fillInCardDeclinedProcessError(): Unit = {
    CreditCard.fillInCardDeclinedProcessError()
  }

  def pay() = {
    val selector = className("js-submit-input")
    assert(pageHasElement(selector))
    click.on(selector)
  }

  private object Name {
    lazy val first = textField(id("name-first"))
    lazy val last = textField(id("name-last"))
  }

  private object DeliveryAddress {
    lazy val country = singleSel(id("country-deliveryAddress"))
    lazy val addressLine1 = textField(id("address-line-one-deliveryAddress"))
    lazy val town = textField(id("town-deliveryAddress"))
    lazy val postCode = textField(id("postCode-deliveryAddress"))

    def fillIn(): Unit = {
      assert(pageHasElement(id("postCode-deliveryAddress")))

      country.value = "GB"
      addressLine1.value = "Kings Place"
      town.value = "London"
      postCode.value = "N1 9GU"
    }
  }

  private object CreditCard {
    lazy val cardNumber = textField(id("cc-num"))
    lazy val cardCvc = textField(id("cc-cvc"))
    lazy val cardExpiryMonth = singleSel(id("cc-exp-month"))
    lazy val cardExpiryYear = singleSel(id("cc-exp-year"))

    def fillIn(): Unit = {
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4242424242424242"
      cardCvc.value = "123"
      cardExpiryMonth.value = "1"
      cardExpiryYear.value = "2019"
    }

    /* https://stripe.com/docs/testing */

    // Charge will be declined with a card_declined code.
    def fillInCardDeclined(): Unit = {
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4000000000000002"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "2019"
      cardCvc.value = "111"
    }

    // Charge will be declined with a card_declined code and a fraudulent reason.
    def fillInCardDeclinedFraud(): Unit = {
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4100000000000019"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "2019"
      cardCvc.value = "111"
    }

    // Charge will be declined with an incorrect_cvc code.
    def fillInCardDeclinedCvc(): Unit = {
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4000000000000127"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "2019"
      cardCvc.value = "111"
    }

    // Charge will be declined with an expired_card code.
    def fillInCardDeclinedExpired(): Unit = {
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4000000000000069"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "2019"
      cardCvc.value = "111"
    }

    // Charge will be declined with a processing_error code.
    def fillInCardDeclinedProcessError(): Unit = {
      assert(pageHasElement(id("cc-cvc")))

      cardNumber.value = "4000000000000119"
      cardExpiryMonth.value = "10"
      cardExpiryYear.value = "2019"
      cardCvc.value = "111"
    }
  }
}
