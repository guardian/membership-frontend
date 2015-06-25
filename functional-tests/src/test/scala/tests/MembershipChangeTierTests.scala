package tests

import com.gu.membership.tags._
import steps.MembershipSteps


class MembershipChangeTierTests extends BaseMembershipTest {

  info("Tests for changing tier")

  feature("A user can downgrade") {

    scenarioWeb("MCT1. A Partner can downgrade to a Friend", UserChangeTier, OptionalTest) {
      implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
        _.IChooseToBecomeAFriend
      }
      .then {
        _.IAmAFriend
      }
    }

    scenarioWeb("MCT2. A Patron can downgrade to a Friend", UserChangeTier, OptionalTest) {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAPatron
        }
        .when {
          _.IChooseToBecomeAFriend
        }
        .then {
          _.IAmAFriend
        }
     }
   }

  feature("A user can upgrade") {

    scenarioWeb("MCT3. A friend can upgrade to a partner", UserChangeTier, CoreTest) {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAFriend
        }
        .when {
          _.IChooseToBecomeAPartner
        }
        .then {
          _.IAmAPartner
        }
    }

    scenarioWeb("MCT4. A Friend can upgrade to a Patron", UserChangeTier, OptionalTest) {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAFriend
        }
        .when {
          _.IChooseToBecomeAPatron
        }
        .then {
          _.IAmAPatron
        }
    }
  }

  scenarioWeb("MCT5. A Partner can cancel membership", UserChangeTier, OptionalTest) {
    implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
        _.ICancelMembership
      }
      .then {
        _.IAmNotAMember
      }
  }

  scenarioWeb("MCT6. An existing friend cannot become a friend again", UserChangeTier, OptionalTest) {
    implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAFriend
      }
      .when {
        _.IGoToTheEventsPage
      }
      .then {
        _.ICantBecomeAFriendAgain
      }
  }

  scenarioWeb("MCT7. An existing partner cannot become a partner again", UserChangeTier, OptionalTest) {
    implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPartner
      }
      .when {
        _.IGoToTheEventsPage
      }
      .then {
        _.ICantBecomeAPartnerAgain
      }
  }

  scenarioWeb("MCT8. An existing patron cannot become a patron again", UserChangeTier, OptionalTest) {
    implicit driver =>
      given {
        MembershipSteps().IAmLoggedInAsAPatron
      }
      .when {
        _.IGoToTheEventsPage
      }
      .then {
        _.ICantBecomeAPatronAgain
      }
  }
}
