package acceptance

import acceptance.util._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class JoinPartnerSpec extends FeatureSpec with Browser
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { /* each test */ Driver.reset() }

  override def beforeAll() = {
    Screencast.storeId()
    Config.printSummary()
  }

  override def afterAll() = {
    Driver.quit()
  }

  private def checkDependenciesAreAvailable = {
    assume(Dependencies.MembershipFrontend.isAvailable,
      s"- ${Dependencies.MembershipFrontend.url} unavaliable! " +
        "\nPlease run membership-frontend server before running tests.")

    assume(Dependencies.IdentityFrontend.isAvailable,
      s"- ${Dependencies.IdentityFrontend.url} unavaliable! " +
        "\nPlease run identity-frontend server before running tests.")
  }

  feature("Become a Partner in UK") {
    scenario("I join as Partner by clicking 'Become a Partner' button on Membership homepage", Acceptance) {
      checkDependenciesAreAvailable

      And("I have the opt out of the ID frontend AB test cookie. (FOR NOW)")
      Driver.addCookie("GU_PROFILE_BETA","0")

      val testUser = new TestUser

      Given("I clicked 'Become a Partner' button on Membership homepage")

      When("I land on 'Identity Register' page")
      val register = new pages.Register(testUser)
      go.to(register)
      assert(register.pageHasLoaded())

      And("I fill in personal details")
      register.fillInPersonalDetails()

      And("I submit the form to create my Identity account")
      register.submit()

      Then("I should land on 'Enter Details' page")
      val enterDetails = new pages.EnterDetails
      assert(enterDetails.pageHasLoaded())

      And("I should have Identity cookies")
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(Driver.cookiesSet.map(_.getName).contains(idCookie)) }

      And("I should be logged in with my Identity account.")
      assert(elementHasText(
        cssSelector(".js-user-displayname"), testUser.username.toLowerCase()))

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
      assert(elementHasText(cssSelector(".js-user-tier"), "Partner"))
    }
  }
}

