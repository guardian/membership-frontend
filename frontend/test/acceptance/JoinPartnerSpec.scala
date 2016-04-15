package acceptance

import acceptance.pages.ThankYou
import acceptance.util._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter, FeatureSpec, GivenWhenThen}
import org.slf4j.LoggerFactory

class JoinPartnerSpec extends FeatureSpec with Browser
  with GivenWhenThen with BeforeAndAfter with BeforeAndAfterAll  {

  def logger = LoggerFactory.getLogger(this.getClass)

  before { /* each test */ Driver.reset() }

  override def beforeAll() {
    Screencast.storeId()
    Config.printSummary()
  }

  override def afterAll() { Driver.quit() }

  private def checkDependenciesAreAvailable = {
    assume(Dependencies.MembershipFrontend.isAvailable,
      s"- ${Dependencies.MembershipFrontend.url} unavaliable! " +
        "\nPlease run membership-frontend server before running tests.")

    assume(Dependencies.IdentityFrontend.isAvailable,
      s"- ${Dependencies.IdentityFrontend.url} unavaliable! " +
        "\nPlease run identity-frontend server before running tests.")
  }

  feature("Become a Partner in UK") {
    scenario("User joins as Partner by clicking 'Become a Partner' button on Membership homepage", Acceptance) {
      checkDependenciesAreAvailable

      val testUser = new TestUser

      Given("users click 'Become a Partner' button on Membership homepage")

      When("they land on 'Identity Frontend' page,")
      val register = pages.Register(testUser)
      go.to(register)
      assert(register.pageHasLoaded)

      And("fill in personal details,")
      register.fillInPersonalDetails()

      And("submit the form to create their Identity account,")
      register.submit()

      Then("they should land on 'Enter Details' page,")
      val enterDetails = pages.EnterDetails(testUser)
      assert(enterDetails.pageHasLoaded)

      And("should have Identity cookies,")
      Seq("GU_U", "SC_GU_U", "SC_GU_LA").foreach { idCookie =>
        assert(Driver.cookiesSet.map(_.getName).contains(idCookie)) }

      And("should be logged in with their Identity account.")
      assert(enterDetails.userIsSignedIn)

      When("Users fill in delivery address details,")
      enterDetails.fillInDeliveryAddress()

      And("fill in card details,")
      enterDetails.fillInCardDetails()

      And("click 'Pay' button,")
      enterDetails.pay()

      Then("they should land on 'Thank You' page,")
      assert(ThankYou.pageHasLoaded)

      And("should be signed in as Partner.")
      assert(ThankYou.userIsSignedInAsPartner)
    }
  }
}

