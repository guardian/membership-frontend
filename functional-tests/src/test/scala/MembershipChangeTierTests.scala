

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
  }

  // cancel membership

  // patron to partner

}
