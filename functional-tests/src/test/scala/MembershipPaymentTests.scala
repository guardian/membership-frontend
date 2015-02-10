

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
    scenarioWeb("9. Non-logged in registered user purchase a subscription") {
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

    scenarioWeb("10. Logged in user can purchase a subscription") {
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

    scenarioWeb("11. A user who pays should be able to see the payment details") {
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

    scenarioWeb("20. User with incorrect card number cannot make a purchase") {
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

    scenarioWeb("21. User with no funds in account cannot make a purchase") {
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

    scenarioWeb("22. User with incorrect CVC in card cannot make a purchase") {
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

    scenarioWeb("24. User with invalid expiry date in card cannot make a purchase") {
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
