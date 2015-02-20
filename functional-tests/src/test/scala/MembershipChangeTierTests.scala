

/**
 * Created by jao on 07/07/2014.
 */
class MembershipChangeTierTests extends BaseMembershipTest {

  info("Tests for changing tier")

  feature("A user can downgrade") {

    scenarioWeb("30. A Partner can downgrade to a Friend") {
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

    scenarioWeb("31. A Patron can downgrade to a Friend") {
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

    scenarioWeb("34. A friend can upgrade to a partner") {
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

//    scenarioWeb("35. A Partner can upgrade to a Patron") {
//      implicit driver =>
//        given {
//          MembershipSteps().IAmLoggedInAsAPartner
//        }
//        .when {
//          _.IChooseToUpgradeToAPatron
//        }
//        .then {
//          _.IAmAPatron
//        }
//    }

    scenarioWeb("36. A Friend can upgrade to a Patron") {
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

  scenarioWeb("37. A Partner can cancel membership") {
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

  scenarioWeb("43. An existing friend cannot become a friend again") {
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

  scenarioWeb("44. An existing partner cannot become a partner again") {
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

  scenarioWeb("45. An existing patron cannot become a patron again") {
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
