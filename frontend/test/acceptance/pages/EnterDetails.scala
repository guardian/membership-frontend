package acceptance.pages

import acceptance.util.{TestUser, Browser, Config}
import Config.baseUrl
import org.scalatest.selenium.Page

case class EnterDetails(val testUser: TestUser) extends Page with Browser {
  val url = s"$baseUrl/join/supporter/enter-details"

  def pageHasLoaded: Boolean = pageHasElement(id("cc-cvc"))

  def userIsSignedIn: Boolean = elementHasText(userDisplayName, testUser.username)

  def fillInDeliveryAddress() { DeliveryAddress.fillIn() }

  def fillInCardDetails() { CreditCard.fillIn() }

  def fillInCardDeclined() { CreditCard.fillInCardDeclined() }

  def fillInCardDeclinedFraud() { CreditCard.fillInCardDeclinedFraud() }

  def fillInCardDeclinedCvc() { CreditCard.fillInCardDeclinedCvc() }

  def fillInCardDeclinedExpired() { CreditCard.fillInCardDeclinedExpired() }

  def fillInCardDeclinedProcessError() { CreditCard.fillInCardDeclinedProcessError() }

  def changeCountry(country: String) { DeliveryAddress.selectCountryCode(country) }

  def currencyHasChanged(): Boolean = elementHasText(currency, "US$")

  def pay() { clickOn(payButton) }

  private object DeliveryAddress {
    val country = id("country-deliveryAddress")
    val addressLine1 = id("address-line-one-deliveryAddress")
    val town = id("town-deliveryAddress")
    val postCode = id("postCode-deliveryAddress")

    def fillIn() {
      setSingleSelectionValue(country, "GB")
      setValue(addressLine1, "Kings Place")
      setValue(town, "London")
      setValue(postCode, "N1 9GU")
    }

    def selectCountryCode(countryCode:  String) { setSingleSelectionValue(country, countryCode) }
  }

  private object CreditCard {
    val cardNumber = id("cc-num")
    val cardCvc = id("cc-cvc")
    val cardExpiryMonth = id("cc-exp-month")
    val cardExpiryYear = id("cc-exp-year")

    private def fillInHelper(cardNum: String) {
      setValue(cardNumber, cardNum)
      setSingleSelectionValue(cardExpiryMonth, "10")
      setSingleSelectionValue(cardExpiryYear, "2019")
      setValue(cardCvc, "111")
    }

    def fillIn() { fillInHelper("4242424242424242") }

    /* https://stripe.com/docs/testing */

    // Charge will be declined with a card_declined code.
    def fillInCardDeclined(): Unit = fillInHelper("4000000000000002")

    // Charge will be declined with a card_declined code and a fraudulent reason.
    def fillInCardDeclinedFraud(): Unit = fillInHelper("4100000000000019")

    // Charge will be declined with an incorrect_cvc code.
    def fillInCardDeclinedCvc(): Unit = fillInHelper("4000000000000127")

    // Charge will be declined with an expired_card code.
    def fillInCardDeclinedExpired(): Unit = fillInHelper("4000000000000069")

    // Charge will be declined with a processing_error code.
    def fillInCardDeclinedProcessError(): Unit = fillInHelper("4000000000000119")
  }

  private val userDisplayName = cssSelector(".js-user-displayname")

  private val payButton = className("js-submit-input")

  private val currency = id("qa-currency-radio")
}
