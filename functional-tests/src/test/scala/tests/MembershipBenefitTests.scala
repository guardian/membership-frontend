package tests

import com.gu.membership.tags.{CoreTest, OptionalTest}
import steps.MembershipSteps

/**
 * Created by jao on 06/06/2014.
 */
class MembershipBenefitTests extends BaseMembershipTest {

  info("Tests for Membership benefits")

  feature("User gets benefits from being a member") {

    scenarioWeb("MB1. Member gets a discount", CoreTest) { implicit driver =>
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.PriceIsHigherThanIfIAmAMember
      }
    }

    scenarioWeb("MB2. Discount gets compared to non-discounted price", OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.OriginalPriceIsComparedToDiscountedPrice
      }
    }
  }

  feature("Membership tab") {

    scenarioWeb("MB3. Membership tab appears if you are a Partner", CoreTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IBecomeAPartner
      }
      .then {
        _.ICanSeeTheMembershipTabForAPartner
      }
    }

    scenarioWeb("MB4. Membership tab appears if you are a Patron", OptionalTest) {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedIn
        }
        .when {
          _.IBecomeAPatron
        }
        .then {
          _.ICanSeeTheMembershipTabForAPatron
        }
    }

    scenarioWeb("MB5. Membership tab appears if you are a Friend", OptionalTest) {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedIn
        }
        .when {
          _.IBecomeAFriend
        }
        .then {
          _.ICanSeeTheMembershipTabForFriend
        }
    }

    scenarioWeb("MB6. Membership tab is an upsell if you are not a member", CoreTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IGoToIdentity
      }
      .then {
        _.theMembershipTabIsAnUpsell
      }
    }
  }
}
