/**
 * Created by jao on 13/11/14.
 */
class MasterclassTests extends BaseMembershipTest {

  info("Basic tests for Masterclasses")

  feature("User sees a list of events") {

    scenarioWeb("47. Visitor sees an event list") {
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
    
    scenarioWeb("48. Member sees an event list") {
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
}
