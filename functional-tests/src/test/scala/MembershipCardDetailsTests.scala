

/**
* Created by jao on 19/06/2014.
*/
class MembershipCardDetailsTests extends BaseMembershipTest {

  info("Tests for card management")

  feature("Manage card payment details") {

    scenarioWeb("User can update card details") { implicit driver =>
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

    scenarioWeb("User can't add an incorrect card") { implicit driver =>
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

    scenarioWeb("User can't add a card with incorrect CVC") { implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
        _.IGoToMembershipTabToChangeDetails

      }
      .then {
        _.ISeeAnErrorWhenMyCVCIsInvalid
      }
    }

    scenarioWeb("User can't add a card with invalid expiry date") {
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
