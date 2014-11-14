/**
 * Created by jao on 13/11/14.
 */
class MasterclassTests extends BaseMembershipTest {

  info("Basic tests for Masterclasses")

  feature("User sees a list of events") {

    scenarioWeb("47. Visitor sees a Masterclass list") {
      implicit driver =>
        given {
          MembershipSteps().IGoToMasterclasses
        }
        .when {
          _.ILand
        }
        .then {
          _.ISeeAListOfEvents
        }
    }

    scenarioWeb("48. Member sees a Masterclass list") {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAPartner
        }
        .when {
          _.IGoToMasterclasses
        }
        .then {
          _.ISeeAListOfEvents
        }
    }
  }

  feature("Masterclass event details") {

    scenarioWeb("49. Visitor sees the details for a Masterclass") {
      implicit driver =>
        given {
          MembershipSteps().IGoToMasterclasses
        }
        .when {
          _.IClickOnAnEvent
        }
        .then {
          _.ISeeTheEventDetails
        }
    }

    scenarioWeb("50. Member sees the details for a Masterclass") {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAPatron
        }
        .when {
         _.IGoToMasterclasses
         .IClickOnAnEvent
        }
        .then {
          _.ISeeTheEventDetails
        }
    }
  }
}
