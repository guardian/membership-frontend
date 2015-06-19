package tests

import com.gu.membership.tags._
import steps.MembershipSteps


class MembershipChangeTierTests extends BaseMembershipTest {

  info("Tests for changing tier")

  feature("A user can downgrade") {

    scenarioWeb("30. A Partner can downgrade to a Friend", UserChangeTier, OptionalTest) {
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

    scenarioWeb("31. A Patron can downgrade to a Friend", UserChangeTier, OptionalTest) {
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

    scenarioWeb("34. A friend can upgrade to a partner", UserChangeTier, OptionalTest) {
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

    scenarioWeb("36. A Friend can upgrade to a Patron", UserChangeTier, OptionalTest) {
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

  scenarioWeb("37. A Partner can cancel membership", UserChangeTier, OptionalTest) {
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

  scenarioWeb("43. An existing friend cannot become a friend again", UserChangeTier, OptionalTest) {
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

  scenarioWeb("44. An existing partner cannot become a partner again", UserChangeTier, OptionalTest) {
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

  scenarioWeb("45. An existing patron cannot become a patron again", UserChangeTier, OptionalTest) {
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
