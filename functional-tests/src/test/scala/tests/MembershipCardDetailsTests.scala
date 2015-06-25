package tests

import com.gu.membership.tags.OptionalTest
import steps.MembershipSteps

/**
* Created by jao on 19/06/2014.
*/
class MembershipCardDetailsTests extends BaseMembershipTest {

  info("Tests for card management")

  feature("Manage card payment details") {

    scenarioWeb("MCD1. User can update card details", OptionalTest) { implicit driver =>
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

    scenarioWeb("MCD2. User can't add an incorrect card", OptionalTest) { implicit driver =>
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

    scenarioWeb("MCD3. User can't add a card with invalid expiry date", OptionalTest) {
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
