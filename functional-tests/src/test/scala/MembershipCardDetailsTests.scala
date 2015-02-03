

/**
* Created by jao on 19/06/2014.
*/
class MembershipCardDetailsTests extends BaseMembershipTest {

  info("Tests for card management")

  feature("Manage card payment details") {

    scenarioWeb("39. User can update card details") { implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
       _.IGoToIdentity
      }
      .then {
        _.ICanUpdateMyCardDetails
      }
    }

    scenarioWeb("40. User can't add an incorrect card") { implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
        _.IGoToMembershipTabToChangeDetails
      }
      .then {
        _.ErrorMessageIsDisplayedWhenIEnterAnInvalidCard
      }
    }

    scenarioWeb("41. User can't add a card with invalid expiry date") {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
        _.IGoToMembershipTabToChangeDetails
      }
      .then {
        _.ISeeAnErrorMessageWhenMyExpiryDateIsInThePast
      }
    }
  }
}
