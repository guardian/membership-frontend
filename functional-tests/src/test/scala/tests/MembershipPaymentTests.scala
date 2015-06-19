package tests

import com.gu.membership.tags.{CoreTest, OptionalTest}
import steps.MembershipSteps

/**
 * Created by jao on 29/05/2014.
 */
class MembershipPaymentTests extends BaseMembershipTest {

  info("Tests for the sprint 2 of Membership")

  feature("Payment option") {

    /*
     In order to become a partner
     As a user
     I want to be able to pay a subscription
     */
    scenarioWeb("MP1. Non-logged in registered user purchase a subscription", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTA
      }
      .then {
        _.IHaveToLogIn
        .IClickOnThePurchaseSubscriptionCTA
        .andICanPurchaseASubscription
      }
    }

    scenarioWeb("MP2. Logged in user can purchase a subscription", CoreTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTA
      }
      .then {
        _.ICanPurchaseASubscription
      }
    }

    scenarioWeb("MP3. A user who pays should be able to see the payment details", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTAForPartner
      }
      .then {
        _.ICanSeeMyPaymentDetails
      }
    }

    scenarioWeb("MP4. User with incorrect card number cannot make a purchase", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTA
      }
      .then {
        _.ErrorMessageIsDisplayedWhenIEnterAnInvalidCard
      }
    }

    scenarioWeb("MP5. User with no funds in account cannot make a purchase", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTA
      }
      .then {
        _.ISeeAnErrorWhenMyCardHasNoFunds
      }
    }

    scenarioWeb("MP6. User with incorrect CVC in card cannot make a purchase", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTA
      }
      .then {
        _.ISeeAnErrorWhenMyCVCIsInvalid
      }
    }

    scenarioWeb("MP7. User with invalid expiry date in card cannot make a purchase", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnThePurchaseSubscriptionCTA
      }
      .then {
        _.ISeeAnErrorMessageWhenMyExpiryDateIsInThePast
      }
    }
  }
}
