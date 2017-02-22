package acceptance.pages

import acceptance.util.{TestUser, Browser, Config}
import Config.baseUrl
import org.scalatest.selenium.Page

case class EnterDetails(val testUser: TestUser) extends Page with Browser {
  val url = s"$baseUrl/join/supporter/enter-details"

  def pageHasLoaded: Boolean = pageHasElement(className("js-sign-in-note"))

  def userIsSignedIn: Boolean = elementHasText(userDisplayName, testUser.username)

  def fillInDeliveryAddress() { DeliveryAddress.fillIn() }

  def clickContinue() = clickOn(className("js-continue-name-address"))

  def changeCountry(country: String) = { DeliveryAddress.selectCountryCode(country) }

  def currencyHasChanged(): Boolean = hiddenElementHasText(currency, "US$")


  // ----- Stripe Methods ----- //

  def payStripe() = clickOn(className("js-stripe-checkout"))

  def switchToStripe() = switchFrame(StripeCheckout.container)

  def stripeCheckoutHasLoaded(): Boolean = pageHasElement(StripeCheckout.container)

  def stripeCheckoutHasCC(): Boolean = pageHasElement(StripeCheckout.cardNumber)

  def stripeCheckoutHasCVC(): Boolean = pageHasElement(StripeCheckout.cardCvc)

  def stripeCheckoutHasExph(): Boolean = pageHasElement(StripeCheckout.cardExp)

  def stripeCheckoutHasSubmit(): Boolean = pageHasElement(StripeCheckout.submitButton)

  def fillInCreditCardPaymentDetailsStripe(): Unit = StripeCheckout.fillIn

  def acceptStripePayment() = StripeCheckout.acceptPayment


  // ----- PayPal Methods ----- //

  def switchToPayPalVariant() = changeUrl(s"$url?countryGroup=uk&paypalTest=with_paypal")

  def payPal() = clickOn(id("paypal-button-checkout"))

  def switchToPayPal() = {
    switchWindow
    switchFrame(PayPalCheckout.container)
  }

  def payPalCheckoutHasLoaded(): Boolean = pageHasElement(PayPalCheckout.loginButton)

  def payPalFillInDetails() = PayPalCheckout.fillIn

  def payPalLogin() = PayPalCheckout.logIn

  def payPalHasPaymentSummary() = pageHasElement(PayPalCheckout.agreeAndPay)

  def payPalSummaryHasCorrectAmount() = elementHasText(PayPalCheckout.paymentAmount, "£49.00")

  def acceptPayPalPayment() = {
    PayPalCheckout.acceptPayment
    switchToParentWindow
  }


  // ----- Page Sections ----- //

  // Handles interaction with the delivery address fields.
  private object DeliveryAddress {
    val country = id("country-deliveryAddress")
    val addressLine1 = id("address-line-one-deliveryAddress")
    val town = id("town-deliveryAddress")
    val postCode = id("postCode-deliveryAddress")

    def fillIn() = {
      setSingleSelectionValue(country, "GB")
      setValue(addressLine1, "Kings Place")
      setValue(town, "London")
      setValue(postCode, "N1 9GU")
    }

    def selectCountryCode(countryCode:  String) { setSingleSelectionValue(country, countryCode) }
  }

  // Handles interaction with the Stripe Checkout overlay.
  private object StripeCheckout {

    val container = name("stripe_checkout_app")
    // Temporary hack to identify elements on Stripe Checkout form using xpath, since the ids are no longer consistently set.
    val cardNumber = xpath("//div[label/text() = \"Card number\"]/input")
    val cardExp = xpath("//div[label/text() = \"Expiry\"]/input")
    val cardCvc = xpath("//div[label/text() = \"CVC\"]/input")
    val submitButton = xpath("//div[button]")

    private def fillInHelper(cardNum: String) = {

      setValueSlowly(cardNumber, cardNum)
      setValueSlowly(cardExp, "1019")
      setValueSlowly(cardCvc, "111")
      
    }

    def fillIn(): Unit = fillInHelper("4242 4242 4242 4242")

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

    def acceptPayment(): Unit = clickOn(submitButton)

  }

  // Handles interaction with the PayPal Express Checkout overlay.
  private object PayPalCheckout {

    val container = name("injectedUl")
    val loginButton = name("btnLogin")
    val emailInput = name("login_email")
    val passwordInput = name("login_password")
    val agreeAndPay = id("confirmButtonTop")
    val paymentAmount = className("formatCurrency")

    // Fills in the sandbox user credentials.
    def fillIn() = {

      setValueSlowly(emailInput, Config.paypalBuyerEmail)
      setValueSlowly(passwordInput, Config.paypalBuyerPassword)

    }

    def logIn() = clickOn(loginButton)

    def acceptPayment() = clickOn(agreeAndPay)

  }

  private val userDisplayName = cssSelector(".js-user-displayname")

  private val payButton = className("js-submit-input")

  private val currency = id("qa-currency-radio")
}
