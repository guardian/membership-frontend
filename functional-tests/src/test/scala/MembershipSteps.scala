import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.{Random, Date, Properties}

import com.gu.automation.support.{Config, TestLogger}
import com.gu.identity.testing.usernames.{Encoder, TestUsernames}
import com.gu.membership.pages._
import org.openqa.selenium.{Cookie, JavascriptExecutor, WebDriver}

case class MembershipSteps(implicit driver: WebDriver, logger: TestLogger) {

  val validCardNumber = "4242424242424242"
  val cardWithNoFunds = "4000000000000341"
  val secondaryCard   = "5555555555554444"

  def IAmLoggedIn = {
    CookieHandler.login(driver)
    this
  }

  def IAmNotLoggedIn = {
    driver.get(Config().getTestBaseUrl())
    CookieHandler.disableAnalytics(driver)
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
//      if (index == 0) return
//      else
////        Assert.assertNotEmpty(page.getEventTitleByIndex(index))
////        Assert.assertNotEmpty(page.getEventLocationByIndex(index))
//        None
//        val eventTime = page.getEventTimeByIndex(index)
//        Assert.assertNotEmpty(eventTime)
//        Assert.assert(isNotInPast(eventTime), true, "The event should be in the future")
//        loop(index - 1)
    }
    this
  }

  def IClickOnAnEvent = {
    new LandingPage(driver).clickEventsButton.clickAnEvent
    this
  }

  def IClickTheFirstEvent = {
    new MasterclassListPage(driver).clickAnEvent()
    this
  }

  def ISeeTheEventDetails = {
    val page = new EventPage(driver)
    Assert.assertNotEmpty(page.getEventDescription)
    Assert.assertNotEmpty(page.getEventLocation)
    Assert.assertNotEmpty(page.getEventPrice)
//    Assert.assertNotEmpty(page.getEventSalesEndTime)
    Assert.assertNotEmpty(page.getEventTime)
    this
  }

  def ISeeTheMasterclassDetails = {
    val page = new MasterClassDetailPage(driver)
    Assert.assertNotEmpty(page.getEventDescription)
    Assert.assertNotEmpty(page.getEventLocation)
    Assert.assertNotEmpty(page.getEventPrice)
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
    new LandingPage(driver).clickEventsButton.clickAnEvent.clickBuyButton
    this
  }

  def ICanPurchaseATicket = {
    val loaded = new EventBritePage(driver).isPageLoaded
    Assert.assert(loaded, true, "Eventbrite page is loaded")
    this
  }

  def IAmRedirectedToTheSignInOrRegisterPage = {
    val loaded = new JoinFlowRegisterOrSignInPage(driver).isPageLoaded
    Assert.assert(loaded, true, "The Sign In or Register page should be loaded")
    this
  }

  def IAmRedirectedToTheChooseTierPage = {
    val loaded = new ChooseTierPage(driver).isPageLoaded
    Assert.assert(loaded, true, "The Choose Tier page should be loaded")
    this
  }

  def IAmRedirectedToTheLoginPage = {
    val loaded = new LoginPage(driver).isPageLoaded
    Assert.assert(loaded, true, "Login page is loaded")
    this
  }

  def ICanBecomeAFriend = {
    new ChooseTierPage(driver).clickFriend
    theFlowSignIn
    becomeFriend
    this
  }

  def theFlowSignIn = {
    new JoinFlowRegisterOrSignInPage(driver).clickRegister
    CookieHandler.register(driver)
  }

  def IHaveInformationInIdentity = {
    IAmLoggedIn
    IGoToIdentity
    IEnterInfoIntoIdentity
    this
  }

  def IEnterInfoIntoIdentity = {
    new IdentityEditPage(driver).clickAccountDetailsTab.enterAddress("somewhere", "nice")
      .enterCountry("Angola").enterPostcode("N1 9GU").enterState("London").enterTown("London").clickSave
    this
  }

  def TheInformationHasBeenLoadedFromIdentity = {
    val details = new PaymentPage(driver).cardWidget
    Assert.assert(details.getAddressLineOne, "somewhere", "Address line 1 should be pulled from identity")
    Assert.assert(details.getAddressLineTwo, "nice", "Address line 2 should be pulled from identity")
    Assert.assert(details.getCounty, "London", "County / state should be pulled from identity")
    Assert.assert(details.getTown, "London", "Town should be pulled from identity")
    Assert.assert(details.getPostCode, "N1 9GU", "Postcode should be pulled from identity")
    // check country
    this
  }

  def IGoToBecomeAPartner = {
    driver.get(Config().getTestBaseUrl())
    new LandingPage(driver).clickJoinButton.clickBecomeAPartner
    this
  }

  def ICanBecomeAPartner = {
    new ChooseTierPage(driver).clickPartner
    theFlowSignIn
    pay
    this
  }

  def ICanSeeTheTicketIframe = {
    Assert.assert(new EventbriteIframe(driver).isIframeLoaded, true, "EventBrite iframe should be loaded")
    this
  }

  def IAmNotRegistered = {
    IAmNotLoggedIn
    this
  }

  def IClickOnThePurchaseSubscriptionCTA = {
    new LandingPage(driver).clickJoinButton.clickBecomeAPartner
    this
  }

  def IClickOnThePurchaseSubscriptionCTAForPartner = {
    IClickOnThePurchaseSubscriptionCTA
    this
  }

  def IHaveToLogIn = {
    IAmLoggedIn
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
    Assert.assert(paidAmount, "£135.00", "Should have paid £15")
    val nextPaymentAmount = thankYouPage.getPaymentAmount
    Assert.assert(nextPaymentAmount, "£135.00", "Next payment should be £15")
    val cardNumber = thankYouPage.getCardNumber
    Assert.assert(cardNumber.endsWith("4242"), true, "Should see correct card details")
    this
  }

  def ErrorMessageIsDisplayedWhenIEnterAnInvalidCard = {
    val errorMessage = new PaymentPage(driver).cardWidget
      .enterCardNumber("1234 5678 9098 7654").enterCardSecurityCode(" ").isErrorMessageDisplayed
    Assert.assert(errorMessage, true, "Invalid card message should be shown")
    this
  }

  def ISeeAnErrorWhenMyCardHasNoFunds = {
    val page = new PaymentPage(driver).cardWidget
    page.submitPayment("", "", "90 York", " Way", "London", "UK", "N19GU", cardWithNoFunds, "111", "12",
      "2018")
    val errorMessage = page.getErrorMessage
    Assert.assert(errorMessage, "This form has errors",
      "We display stripe's error message correctly")
    this
  }

  def ISeeAnErrorWhenMyCVCIsInvalid = {
    new PaymentPage(driver).cardWidget.enterCardNumber(validCardNumber)
      .enterCardSecurityCode(" ").enterCardExpirationMonth("1").clickSubmitPayment
    val errorMessage = new CreditCardWidget(driver).isErrorMessageDisplayed
    Assert.assert(errorMessage, true, "We should display a valid CVC error message")
    this
  }

  def ISeeAnErrorMessageWhenMyExpiryDateIsInThePast = {
    val errorMessage = new PaymentPage(driver).cardWidget.enterCardNumber(validCardNumber)
      .enterCardSecurityCode("666").enterCardExpirationMonth("1")
      .enterCardExpirationYear("2015").focusOnCvc.isErrorMessageDisplayed
    Assert.assert(errorMessage, true, "We should display an error message when the card is expired")
    this
  }

  def PriceIsHigherThanIfIAmAMember = {
    val price = new EventPage(driver).getEventPrice
    if (price != "Free") {
      val initialPrice = price.replace("£", "").toInt
      IAmLoggedIn
      IClickOnAnEvent
      val discountedPrice = new EventPage(driver).getDiscountedEvent.replace("Partners/Patrons £", "").toInt
      Assert.assert(initialPrice > discountedPrice, true, "Member receives a discount")
    }
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
    new LandingPage(driver).clickJoinButton.clickBecomeAPatron
    ICanPurchaseASubscription
    this
  }

  def IBecomeAFriend = {
    new LandingPage(driver).clickJoinButton.clickBecomeAFriend.enterPostCode("N19GU").clickJoinNow
    this
  }

  def ICancelMembership = {
    new LandingPage(driver).clickAccountControl.clickEditProfile.clickChangeTier.clickCancelLink
      .clickConfirmCancellation.clickBackToMyProfile
    this
  }

  def ICanSeeTheMembershipTabForAPartner = {
   ICanSeeTheMembershipTab("partner", "£135.00")
  }

  def ICanSeeTheMembershipTabForAPatron = {
    ICanSeeTheMembershipTab("patron", "£540.00")
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

  def theMembershipTabIsAnUpsell = {
    Assert.assert(new IdentityEditPage(driver).isAnUpsell, true, "Membership tab is an upsell")
    this
  }

  def IGoToMembershipTabToChangeDetails = {
    driver.get(Config().getUserValue("membershipEdit"))
    new IdentityEditPage(driver).clickChangeButton
    this
  }

  def ICanUpdateMyCardDetails = {
    val page = new IdentityEditPage(driver)
    page.clickMembershipTab.clickChangeButton.enterCardNumber(secondaryCard)
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
    Assert.assert(page.getNewPackage, "Friend", "The new package should be Friend")
  }

  def IAmAPartner = verifyTier("£135.00")

  def IAmAPatron = verifyTier("£540.00")

  def IAmNotAMember {
    Assert.assert(new IdentityEditPage(driver).isMembershipCancelled, true, "Membership should be cancelled")
  }

  def ICantBecomeAPatronAgain = {
    new EventsListPage(driver).clickPricing.clickBecomeAPatron
    Assert.assert(new ChangeTierPage(driver).isPageLoaded, true, "A Friend can't become a Friend twice")
  }

  def ICantBecomeAPartnerAgain = {
    new EventsListPage(driver).clickPricing.clickBecomeAPartner
    Assert.assert(new ChangeTierPage(driver).isPageLoaded, true, "A Friend can't become a Friend twice")
  }

  def ICantBecomeAFriendAgain = {
    new EventsListPage(driver).clickLogo.clickPricing.clickBecomeAFriend
    Assert.assert(new ChangeTierPage(driver).isPageLoaded, true, "A Friend can't become a Friend twice")
  }

  def IGoToMasterclasses = {
    driver.get(Config().getUserValue("masterclasses"))
    this
  }

  def ISearchFor(keyword: String) = {
    new MasterclassListPage(driver).search(keyword)
    this
  }

  def SearchResultsMatch(keyword: String) = {
    println("keyword: " + keyword + " string: " + new MasterclassListPage(driver).getEventTitleByIndex(0))
    val found = new MasterclassListPage(driver).getEventTitleByIndex(0).contains(keyword)
    Assert.assert(found, true, "First result should contain keyword")
  }

  private def verifyTier(yearlyPayment: String) = {
    val page = new ThankYouPage(driver)
    Assert.assert(isInFuture(page.getNextPaymentDate), true, "The next payment date should be in the future")
    Assert.assert(page.getAmountPaidToday, yearlyPayment, "Amount paid today should be " + yearlyPayment)
    Assert.assert(page.getPaymentAmount, yearlyPayment, "The yearly payment should be the same as the current payment")
    Assert.assert(page.getCardNumber.endsWith("4242"), true, "The card number should be correct")
  }

  private def pay: ThankYouPage = new PaymentPage(driver).cardWidget.submitPayment("", "", "90 York",
    "Way", "UK", "London", "N19GU", validCardNumber, "111", "12", "2021")

  private def becomeFriend = new PaymentPage(driver).cardWidget.enterPostCode("N1 9GU").clickSubmitPayment

  private def isInFuture(dateTime: String) = {
    // TODO James Oram MEM-141 should make this not fail occasionally
    val sdf = new SimpleDateFormat("d MMMM yyyy")
    sdf.parse(dateTime).after(new Date())
  }

  private def isNotInPast(dateTime: String) = {
    isInFuture(dateTime) || new SimpleDateFormat("d MMMM yyyy").format(new Date()).equals(dateTime)
  }
}

object CookieHandler {

  var loginCookie: Option[Cookie] = None
  var secureCookie: Option[Cookie] = None
  val surveyCookie = new Cookie("gu.test", "test")

  def login(driver: WebDriver) {
    driver.get(Config().getUserValue("identityReturn"))
    disableAnalytics(driver)
    new LoginPage(driver).clickRegister
    register(driver)
  }

  def register(driver: WebDriver) {
    driver.manage().addCookie(surveyCookie)
    val propertyName="identity.test.users.secret"

    val file: String = "/etc/gu/membership-keys.conf"

    val prop = new Properties()
    prop.load(new FileInputStream(file))

    val secret = prop.getProperty(propertyName).replace("\"","")

    val usernames = TestUsernames(Encoder.withSecret(secret))

    val salt: Array[Byte] = new Array[Byte](2)

    new Random().nextBytes(salt)
    val user = usernames.generate(salt)
    val password = scala.util.Random.alphanumeric.take(10).mkString
    val email = user + "@testme.com"
    new RegisterPage(driver).enterFirstName(user).enterLastName(user).enterEmail(email)
      .enterPassword(password).enterUserName(user).clickSubmit
  }

  def disableAnalytics(driver: WebDriver): Unit = {
    driver.asInstanceOf[JavascriptExecutor].executeScript("document.cookie = \"ANALYTICS_OFF_KEY=1; domain=.thegulocal.com; path=/; secure\"")
  }
}
