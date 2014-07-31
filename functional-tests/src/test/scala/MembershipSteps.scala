import java.text.SimpleDateFormat
import java.util.Date

import com.gu.automation.support.{Config, TestLogger}
import com.gu.membership.pages._
import org.openqa.selenium.{Cookie, WebDriver}

case class MembershipSteps(implicit driver: WebDriver, logger: TestLogger) {

  val validCardNumber = "4242 4242 4242 4242"
  val cardWithNoFunds = "4000000000000341"

  def IAmLoggedIn = {
    CookieHandler.login(driver)
    this
  }

  def IAmNotLoggedIn = {
    driver.get(Config().getTestBaseUrl())
    this
  }

  def ILand(implicit logger: TestLogger) = {
    this
  }

  def IGoToTheEventsPage = {
    new LandingPage(driver).clickEventsButton
    this
  }

  def TitleIsDisplayed = {
    val title = new LandingPage(driver).getTitle()
    Assert.assert(title, "Membership")
    this
  }

  def ISeeAListOfEvents = {
    val page = new EventsListPage(driver)
    val eventCount = page.getEventsListSize - 1
    Assert.assert(eventCount > 6, true, "There are 6 or more events")
    loop(eventCount)

    def loop(index: Int) {
      if (index == 0) return
      else
        Assert.assertNotEmpty(page.getEventTitleByIndex(index))
        Assert.assertNotEmpty(page.getEventLocationByIndex(index))
        val eventTime = page.getEventTimeByIndex(index)
        Assert.assertNotEmpty(eventTime)
        Assert.assert(isInFuture(eventTime), true, "The event should be in the future")
        loop(index - 1)
    }
    this
  }

  def IClickOnAnEvent = {
    new LandingPage(driver).clickEventsButton.clickLastEvent
    this
  }

  def ISeeTheEventDetails = {
    val page = new EventPage(driver)
    Assert.assertNotEmpty(page.getEventDescription)
    Assert.assertNotEmpty(page.getEventLocation)
    Assert.assertNotEmpty(page.getEventPrice)
    Assert.assertNotEmpty(page.getEventSalesEndTime)
    Assert.assertNotEmpty(page.getEventTime)
    this
  }

  def TheDetailsAreTheSameAsOnTheEventProvider = {
    val page = new EventPage(driver)
    val eventName = page.getEventName
    // assumes we are logged in
    val eventBritePage = page.clickBuyButton
    Assert.assert(eventBritePage.getEventName.contains(eventName),
      true, "The event name should be the same on Eventbrite")
    this
  }

  def IClickThePurchaseButton = {
    new LandingPage(driver).clickEventsButton.clickLastEvent.clickBuyButton
    this
  }

  def ICanPurchaseATicket = {
    val loaded = new EventBritePage(driver).isPageLoaded
    Assert.assert(loaded, true, "Eventbrite page is loaded")
    this
  }

  def IAmRedirectedToTheLoginPage = {
    val loaded = new LoginPage(driver).isPageLoaded
    Assert.assert(loaded, true, "Login page is loaded")
    this
  }

  def IAmNotRegistered = {
    IAmNotLoggedIn
    this
  }

  def IClickOnThePurchaseSubscriptionCTA = {
    new LandingPage(driver).clickJoinButton.clickBecomeAPartner.clickJoinButton
    this
  }

  def IClickOnThePurchaseSubscriptionCTAForPartner = {
    IClickOnThePurchaseSubscriptionCTA
    this
  }

  def IHaveToLogIn = {
    IAmLoggedIn
    driver.get(Config().getUserValue("partnerPayment"))
    this
  }

  def andICanPurchaseASubscription = {
    val thankYouPage = pay
    val startDate = thankYouPage.getStartDate
    val nextPaymentDate = thankYouPage.getNextPaymentDate
    Assert.assert(isInFuture(nextPaymentDate), true, "Next payment date should be in the future")
    Assert.assert(isInFuture(startDate), false, "The Start Date should not be in the future")
    val paidAmount = thankYouPage.getAmountPaidToday
    Assert.assertNotEmpty(paidAmount, "There paid amount should not be empty")
    val nextPaymentAmount = thankYouPage.getPaymentAmount
    Assert.assertNotEmpty(nextPaymentAmount, "The next payment amount should not be empty")
    val cardNumber = thankYouPage.getCardNumber
    Assert.assertNotEmpty(cardNumber, "The card number should not be empty")
    this
  }

  def ICanPurchaseASubscription = {
    andICanPurchaseASubscription
    this
  }

  def ICanSeeMyPaymentDetails = {
    val thankYouPage = pay
    val paidAmount = thankYouPage.getAmountPaidToday
    Assert.assert(paidAmount, "£15.00", "Should have paid £15")
    val nextPaymentAmount = thankYouPage.getPaymentAmount
    Assert.assert(nextPaymentAmount, "£15.00", "Next payment should be £15")
    val cardNumber = thankYouPage.getCardNumber
    Assert.assert(cardNumber.endsWith("4242"), true, "Should see correct card details")
    this
  }

  def ErrorMessageIsDisplayedWhenIEnterAnInvalidCard = {
    val errorMessage = new PaymentPage(driver).cardWidget
      .enterCardNumber("1234 5678 9098 7654").enterCardSecurityCode(" ").getErrorMessage
    Assert.assert(errorMessage, "Sorry, the card number that you have entered is incorrect. Please check and retype.",
      "Invalid card message should be shown")
    this
  }

  def ISeeAnErrorWhenMyCardHasNoFunds = {
    val page = new PaymentPage(driver).cardWidget
    page.submitPayment("Test", "Automation", "90 York", " Way", "London", "UK", "N19GU", cardWithNoFunds, "111", "12",
      "2018")
    val errorMessage = page.getErrorMessage
    Assert.assert(errorMessage, "We're sorry. Your card has been declined.",
      "We display stripe's error message correctly")
    this
  }

  def ISeeAnErrorWhenMyCVCIsInvalid = {
    val errorMessage = new PaymentPage(driver).cardWidget.enterCardNumber(validCardNumber)
      .enterCardSecurityCode(" ").enterCardExpirationMonth("1").getErrorMessage
    Assert.assert(errorMessage, "Sorry, the security code that you have entered is incorrect. Please check and retype.",
      "We should display a valid CVC error message")
    this
  }

  def ISeeAnErrorMessageWhenMyExpiryDateIsInThePast = {
    val errorMessage = new PaymentPage(driver).cardWidget.enterCardNumber(validCardNumber)
      .enterCardSecurityCode("666").enterCardExpirationMonth("1")
      .enterCardExpirationYear("2014").focusOnCvc.getErrorMessage
    Assert.assert(errorMessage, "Sorry, the expiry date that you have entered is invalid. Please check and re-enter."
      , "We should display an error message when the card is expired")
    this
  }

  def PriceIsHigherThanIfIAmAMember = {
    val initialPrice = new EventPage(driver).getEventPrice
    IAmLoggedIn
    IClickOnAnEvent
    val discountedPrice = new EventPage(driver).getDiscountedEvent
    Assert.assert(initialPrice > discountedPrice, true, "Member receives a discount")
    this
  }

  def OriginalPriceIsComparedToDiscountedPrice = {
    Assert.assertNotEmpty(new EventPage(driver).getOriginalPrice, "There is an original price displayed next to the discounted price")
    this
  }

  def ICanRegisterAndPurchaseASubscription = {
    val user = "test_" + System.currentTimeMillis()
    val correct = new LoginPage(driver).clickRegister.enterEmail(user + "@testme.com")
      .enterPassword(scala.util.Random.alphanumeric.take(10).mkString).enterUserName(user)
      .clickSubmit.clickCompleteRegistration.isPageLoaded
    Assert.assert(correct, true, "Newly-registered user is redirected to the ticket purchase page")
    this
  }

  def IBecomeAPartner = {
    IClickOnThePurchaseSubscriptionCTA
    ICanPurchaseASubscription
    this
  }

  def IBecomeAPatron = {
    new LandingPage(driver).clickJoinButton.clickBecomeAPatron.clickJoinButton
    ICanPurchaseASubscription
    this
  }

  def IBecomeAFriend = {
    new LandingPage(driver).clickJoinButton.clickBecomeAFriend.clickJoinFriendButton.enterFirstName("Test")
      .enterLastName("Automation").enterPostCode("N19GU").clickSubmitPayment
    this
  }

  def ICanSeeTheMembershipTabForAPartner = {
   ICanSeeTheMembershipTab("partner", "15.00")
  }

  def ICanSeeTheMembershipTabForAPatron = {
    ICanSeeTheMembershipTab("patron", "60.00")
  }

  def ICanSeeTheMembershipTabForFriend = {
    IGoToIdentity
    val page = new IdentityEditPage(driver).clickMembershipTab
    Assert.assert(page.getMembershipTier.toLowerCase.contains("friend"), true, "Membership plan should be friend")
    Assert.assert(page.getPaymentCost, "FREE", "Cost should be £FREE")
  }

  def ICanSeeTheMembershipTab(tier: String, price: String) = {
    IGoToIdentity
    val page = new IdentityEditPage(driver).clickMembershipTab
    Assert.assert(page.getMembershipTier.toLowerCase.contains(tier), true, "Membership plan should be " + tier)
    Assert.assert(page.getPaymentCost, price, "Cost should be £" + price)
    Assert.assert(page.getCardDetails.endsWith("4242"), true, "Card should be correct")
    Assert.assertNotEmpty(page.getStartDate, "Start date should not be empty")
    this
  }

  def IGoToIdentity = {
    driver.get(Config().getUserValue("accountEdit"))
    this
  }

  def IDontSeeTheMembershipTab = {
    Assert.assert(new IdentityEditPage(driver).isMembershipTabVisible, false, "Membership tab should not be visible")
    this
  }

  def IGoToMembershipTabToChangeDetails = {
    driver.get(Config().getUserValue("membershipEdit"))
    new IdentityEditPage(driver).clickChangeButton
    this
  }

  def ICanUpdateMyCardDetails = {
    val page = new IdentityEditPage(driver)
    page.clickMembershipTab.clickChangeButton.enterCardNumber(validCardNumber)
      .enterCardSecurityCode("343").enterCardExpirationMonth("11").enterCardExpirationYear("2018").clickUpdateCardDetails
    val success = new IdentityEditPage(driver).isSuccessFlashMessagePresent
    Assert.assert(success, true, "The card update should be successful")
    this
  }

  def IAmLoggedInAsAPartner = {
    IAmLoggedIn
    IBecomeAPartner
    this
  }

  def IAmLoggedInAsAPatron = {
    IAmLoggedIn
    IBecomeAPatron
  }

  def IAmLoggedInAsAFriend = {
    IAmLoggedIn
    IBecomeAFriend
  }

  def IChooseToBecomeAFriend = {
    new ThankYouPage(driver).clickAccountControl.clickEditProfile.clickMembershipTab.clickChangeTier
      .clickBecomeAFriend.clickContinue
    this
  }

  def IChooseToBecomeAPartner = {
    new ThankYouPage(driver).clickAccountControl.clickEditProfile.clickMembershipTab.clickChangeTier
      .clickBecomeAPartner.creditCard.submitPayment("90 York", "Way", "UK", "London", "N19GU", validCardNumber, "111",
        "12", "2031")
    new UpgradePage(driver).clickSubmit
    this
  }

  def IChooseToBecomeAPatron = {
    new ThankYouPage(driver).clickAccountControl.clickEditProfile.clickMembershipTab.clickChangeTier
      .clickBecomeAPatron.creditCard.submitPayment("90 York", "Way", "UK", "London", "N19GU", validCardNumber, "111",
        "12", "2031")
    new UpgradePage(driver).clickSubmit
    this
  }

  def IAmAFriend = {
    val page = new DowngradeConfirmationPage(driver)
    Assert.assert(page.getCurrentPackage.equals("Friend plan"), false, "Current package should not be friend")
    Assert.assert(page.getEndDate, page.getStartDate, "The new date should be the same as the old date")
    Assert.assert(page.getNewPackage, "Friend plan", "The new package should be Friend")
  }

  def IAmAPartner = verifyTier("£135.00")

  def IAmAPatron = verifyTier("£540.00")

  private def verifyTier(yearlyPayment: String) = {
    val page = new ThankYouPage(driver)
    Assert.assert(isInFuture(page.getNextPaymentDate), true, "The next payment date should be in the future")
    Assert.assert(page.getAmountPaidToday, yearlyPayment, "Amount paid today should be " + yearlyPayment)
    Assert.assert(page.getPaymentAmount, yearlyPayment, "The yearly payment should be the same as the current payment")
    Assert.assert(page.getCardNumber.endsWith("4242"), true, "The card number should be correct")
  }

  private def pay: ThankYouPage = new PaymentPage(driver).cardWidget.submitPayment("Test", "Automation", "90 York",
    "Way", "UK", "London", "N19GU", validCardNumber, "111", "12", "2031")

  private def isInFuture(dateTime: String) = {
    // TODO James Oram MEM-141 should make this not fail occasionally
    new SimpleDateFormat("dd MMMM yyyy").parse(dateTime).after(new Date())
  }
}

object CookieHandler {

  var loginCookie: Option[Cookie] = None
  var secureCookie: Option[Cookie] = None
  val surveyCookie = new Cookie("gu.test", "test")

  def login(driver: WebDriver) = {

    driver.get(Config().getUserValue("identityReturn"))
    driver.manage().addCookie(surveyCookie)
    val user = System.currentTimeMillis().toString
    val password = scala.util.Random.alphanumeric.take(10).mkString
    val email = user + "@testme.com"
    new LoginPage(driver).clickRegister.enterEmail(email)
    .enterPassword(password).enterUserName(user).clickSubmit.clickCompleteRegistration
    ////////////////////////////////////////////////////////////
//    driver.get(Config().getUserValue("identityReturn"))
//    new LoginPage(driver).enterEmail(email).enterPassword(password)
//    Thread.sleep(5000)
//    driver.get(Config().getTestBaseUrl())
    ////////////////////////////////////////////////////////////

//    this.synchronized {
//      if (None == CookieHandler.loginCookie) {
//        driver.get(Config().getUserValue("identityReturn"))
//        driver.manage().addCookie(surveyCookie)
//        val user = System.currentTimeMillis().toString
//        new LoginPage(driver).clickRegister.enterEmail(user + "@testme.com")
//          .enterPassword(scala.util.Random.alphanumeric.take(10).mkString).enterUserName(user).clickSubmit.clickCompleteRegistration
//        driver.get(Config().getTestBaseUrl())
//        CookieHandler.loginCookie = Option(driver.manage().getCookieNamed("GU_U"))
//        CookieHandler.secureCookie = Option(driver.manage().getCookieNamed("SC_GU_U"))
//      } else {
//        driver.get(Config().getTestBaseUrl())
//        driver.manage().addCookie(surveyCookie)
//        driver.manage().addCookie(CookieHandler.loginCookie.get)
//        driver.manage().addCookie(CookieHandler.secureCookie.get)
//      }
//    }
  }
}