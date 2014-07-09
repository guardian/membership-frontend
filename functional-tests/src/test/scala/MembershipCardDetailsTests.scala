

/**
* Created by jao on 19/06/2014.
*/
class MembershipCardDetailsTests extends BaseMembershipTest {

  info("Tests for card management")

  feature("Manage card payment details") {

    scenarioWeb("User can update card details") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
       _.IGoToIdentity
      }
      .then {
        _.ICanUpdateMyCardDetails
      }
    }

    scenarioWeb("User can't add an incorrect card") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IGoToMembershipTabToChangeDetails
      }
      .then {
        _.ErrorMessageIsDisplayedWhenIEnterAnInvalidCard
      }
    }

    scenarioWeb("User can't add a card with incorrect CVC") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IGoToMembershipTabToChangeDetails

      }
      .then {
        _.ISeeAnErrorWhenMyCVCIsInvalid
      }
    }

    scenarioWeb("User can't add a card with invalid expiry date") {
      given {
        MembershipSteps().IAmLoggedIn
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
