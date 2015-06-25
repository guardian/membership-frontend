package tests

import com.gu.membership.tags._
import steps.MembershipSteps

class MasterclassTests extends BaseMembershipTest {

  info("Basic tests for Masterclasses")

  feature("User sees a list of events") {

    scenarioWeb("M1. Visitor sees a Masterclass list", EventListTest, OptionalTest) {
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

    scenarioWeb("M2. Member sees a Masterclass list", EventListTest, OptionalTest) {
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

    scenarioWeb("M3. Visitor sees the details for a Masterclass", EventDetailTest, OptionalTest) {
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

    scenarioWeb("M4. Member sees the details for a Masterclass", EventDetailTest, OptionalTest) {
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

}
