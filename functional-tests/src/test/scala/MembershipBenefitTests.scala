

/**
 * Created by jao on 06/06/2014.
 */
class MembershipBenefitTests extends BaseMembershipTest {

  info("Tests for Membership benefits")

  feature("User gets benefits from being a member") {

    scenarioWeb("25. Member gets a discount") { implicit driver =>
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

    scenarioWeb("26. Discount gets compared to non-discounted price") {
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

    scenarioWeb("28. Membership tab appears if you are a Partner") {
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

    scenarioWeb("32. Membership tab appears if you are a Patron") {
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

    scenarioWeb("33. Membership tab appears if you are a Friend") {
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

    scenarioWeb("29. Membership tab is an upsell if you are not a member") {
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
