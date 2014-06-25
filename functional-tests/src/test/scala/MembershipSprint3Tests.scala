

/**
 * Created by jao on 06/06/2014.
 */
class MembershipSprint3Tests extends BaseMembershipTest {

  info("Tests for Membership sprint 3")

  scenarioWeb("25. Member gets a discount") {
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

  scenarioWeb("28. Membership tab appears if you are a member") {
    given {
      MembershipSteps().IAmLoggedIn
    }
    .when {
      _.IBecomeAPartner
    }
    .then {
      _.ICanSeeTheMembershipTab
    }
  }

  scenarioWeb("29. Membership tab does not appear if you are not a member") {
    given {
      MembershipSteps().IAmNotLoggedIn
    }
    .when {
      _.IGoToIdentity
    }
    .then {
      _.IDontSeeTheMembershipTab
    }
  }
}
