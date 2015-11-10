package acceptance

import org.openqa.selenium.WebDriver
import org.scalatest.selenium.WebBrowser
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}

class JoinPartnerSpec extends FeatureSpec
  with WebBrowser with Util with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  implicit lazy val driver: WebDriver = Config.driver

  before {
    resetDriver()
    Config.printSummary()
  }

  override def afterAll(): Unit = { quit() }

  feature("Become a Partner Member") {
    scenario("I join as Partner by clicking 'Become a Partner' button on Membership homepage", Acceptance) {

      info("Stage: " + Config.stage + " " + Config.baseUrl)

      Given("I clicked 'Become a Partner' button on Membership homepage")

      When("I land on 'Identity Register' page")
      val register = new pages.Register
      go.to(register)
      assert(register.pageHasLoaded())

      And("I fill in personal details")
      register.fillInPersonalDetails()

      And("I submit the form to create my Identity account")
      register.submit()

      Then("I should land back on Membership page")
      val membership = new pages.Membership
      assert(membership.pageHasLoaded())

      And("I should have Identity cookies")
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(cookiesSet.map(_.getName).contains(idCookie)) }

      And("I should be logged in with my newly created account.")
      assert(membership.userDisplayName == TestUser.specialString.toLowerCase)

      When("I click on 'Become a Partner' button")
      membership.becomePartner

      Then("I should land on 'Enter Details' page")
      val enterDetails = new pages.EnterDetails
      assert(enterDetails.pageHasLoaded())

      And("I should still be logged in with my Identity account.")
      assert(enterDetails.userDisplayName == TestUser.specialString.toLowerCase)

      When("I fill in delivery address details")
      enterDetails.fillInDeliveryAddress()

      And("I fill in card details")
      enterDetails.fillInCardDetails()

      And("I click 'Pay' button")
      enterDetails.pay()

      Then("I should land on 'Thank You' page")
      val thankYou = new pages.ThankYou
      assert(thankYou.pageHasLoaded())

      And("I should be signed in as Partner.")
      assert(thankYou.userDisplayName == TestUser.specialString.toLowerCase)
      assert(thankYou.userTier == "Partner")
    }
  }
}

