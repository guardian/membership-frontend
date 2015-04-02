import com.gu.membership.tags._

class MasterclassTests extends BaseMembershipTest {

  info("Basic tests for Masterclasses")

  feature("User sees a list of events") {

    scenarioWeb("47. Visitor sees a Masterclass list", EventListTest) {
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

    scenarioWeb("48. Member sees a Masterclass list", EventListTest) {
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

    scenarioWeb("49. Visitor sees the details for a Masterclass", EventDetailTest) {
      implicit driver =>
        given {
          MembershipSteps().IGoToMasterclasses
        }
        .when {
          _.IClickTheFirstEvent
        }
        .then {
          _.ISeeTheMasterclassDetails
        }
    }

    scenarioWeb("50. Member sees the details for a Masterclass", EventDetailTest) {
      implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAPatron
        }
        .when {
         _.IGoToMasterclasses
         .IClickTheFirstEvent
        }
        .then {
          _.ISeeTheMasterclassDetails
        }
    }
  }

  // TODO this is disabled as the feature is not yet complete
//  feature("A user can search Masterclasses") {
//
//    scenarioWeb("51. A non-member can search Masterclasses") {
//      implicit driver =>
//        given {
//          MembershipSteps().IAmNotLoggedIn
//        }
//        .when {
//          MembershipSteps().IGoToMasterclasses
//          _.ISearchFor("food")
//        }
//        .then {
//          _.SearchResultsMatch("food")
//        }
//    }
//  }
}
