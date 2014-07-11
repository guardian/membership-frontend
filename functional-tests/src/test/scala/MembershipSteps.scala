import com.gu.automation.support.TestLogger
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
    // TODO move the data to a config file
    driver.get("https://membership.theguardian.com/")
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
    driver.get("https://membership.theguardian.com/join/partner/payment")
    this
  }

  def andICanPurchaseASubscription = {
    val thankYouPage = pay
    val startDate = thankYouPage.getStartDate
    val nextPaymentDate = thankYouPage.getNextPaymentDate
    // TODO James Oram verify dates  correct format is implemented
    // TODO James Oram we don't really care what the numbers are here, but there will be a test for each tier
    val paidAmount = thankYouPage.getAmountPaidToday
    Assert.assertNotEmpty(paidAmount, "There paid amount should not be empty")
    val nextPaymentAmount = thankYouPage.getMonthlyPayment
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
    val nextPaymentAmount = thankYouPage.getMonthlyPayment
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
    page.submitPayment(cardWithNoFunds, "111", "12", "2018")
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
    new LandingPage(driver).clickJoinButton.clickBecomeAFriend.clickJoinButton
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
    // TODO James Oram move this to a config file
    driver.get("https://profile.theguardian.com/account/edit")
    this
  }

  def IDontSeeTheMembershipTab = {
    Assert.assert(new IdentityEditPage(driver).isMembershipTabVisible, false, "Membership tab should not be visible")
    this
  }

  def IGoToMembershipTabToChangeDetails = {
    driver.get("https://profile.theguardian.com/membership/edit")
    new IdentityEditPage(driver).clickChangebutton
    this
  }

  def ICanUpdateMyCardDetails = {
    val page = new IdentityEditPage(driver)
    page.clickMembershipTab.clickChangebutton.enterCardNumber(validCardNumber)
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

  def IChooseToBecomeAFriend = {
    new ThankYouPage(driver).clickAccountControl.clickEditProfile.clickMembershipTab.clickChangeTier
      .clickBecomeAFriend.clickContinue
    this
  }

  def IAmAFriend = {
    val page = new DowngradeConfirmationPage(driver)
    Assert.assert(page.getCurrentPackage.equals("Friend plan"), false, "Current package should not be friend")
    Assert.assert(page.getEndDate, page.getStartDate, "The new date should be the same as the old date")
    Assert.assert(page.getNewPackage, "Friend plan", "The new package should be Friend")
  }

  private def pay: ThankYouPage = new PaymentPage(driver).cardWidget.submitPayment(validCardNumber, "111", "12", "2031")

  private def isInFuture(dateTime: String): Boolean = {
//    val sdf = new SimpleDateFormat("d MMMM y, h:mmaa")
//    sdf.parse(dateTime.replaceFirst("[rd|th|nd|st]", "")
//      .replaceFirst("[rd|th|nd|st]", "")).after(new Date())
    // TODO James Oram this is disabled due to MEM-141
    true
  }
}

object CookieHandler {

  var loginCookie: Option[Cookie] = None
  var secureCookie: Option[Cookie] = None

  def login(driver: WebDriver) = {
    this.synchronized {
      if (None == CookieHandler.loginCookie) {
        // TODO move the data to a config file
        driver.get("https://profile.theguardian.com/signin?returnUrl=https%3A%2F%2Fmembership.theguardian.com")
        val user = System.currentTimeMillis().toString
        new LoginPage(driver).clickRegister.enterEmail(user + "@testme.com")
          .enterPassword(scala.util.Random.alphanumeric.take(10).mkString).enterUserName(user).clickSubmit.clickCompleteRegistration
        driver.get("https://membership.theguardian.com")
        CookieHandler.loginCookie = Option(driver.manage().getCookieNamed("GU_U"))
        CookieHandler.secureCookie = Option(driver.manage().getCookieNamed("SC_GU_U"))
      } else {
        driver.get("https://membership.theguardian.com")
        driver.manage().addCookie(CookieHandler.loginCookie.get)
        driver.manage().addCookie(CookieHandler.secureCookie.get)
      }
    }
  }
}