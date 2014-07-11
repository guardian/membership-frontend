

/**
 * Created by jao on 07/07/2014.
 */
class MembershipDowngradeTests extends BaseMembershipTest {

  info("Tests for downgrade paths")

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
  // cancel membership

  // patron to friend

  // patron to partner

}
