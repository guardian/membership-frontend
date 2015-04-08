import com.gu.membership.tags._

class MembershipEventTests extends BaseMembershipTest {

  feature("See event list") {

      /*
       I order to view an event's details
       As a user
       I want to see a list of events
       */
      scenarioWeb("1. Logged in user sees event list", EventListTest) {
        implicit driver =>
        given {
          MembershipSteps().IAmLoggedIn
        }
        .when {
          _.IGoToTheEventsPage
        }
        .then {
          _.ISeeAListOfEvents
        }
      }

      scenarioWeb("2. Non logged in user sees event list", EventListTest) {
        implicit driver =>
        given {
          MembershipSteps().IAmNotLoggedIn
        }
        .when {
          _.IGoToTheEventsPage
        }
        .then {
          _.ISeeAListOfEvents
        }
      }
    }

    feature("See details for an event") {
      /*
       In order to choose an event that I like
       As a user
       I want to see the details of an event
       */
      scenarioWeb("3. Logged in user sees details for an event", EventDetailTest) {
        implicit driver =>
        given {
          MembershipSteps().IAmLoggedIn
        }
        .when {
          _.IClickOnAnEvent
        }
        .then {
          _.ISeeTheEventDetails
        }
      }

      scenarioWeb("4. Non logged in user sees details for an event", EventDetailTest) {
        implicit driver =>
        given {
          MembershipSteps().IAmNotLoggedIn
        }
        .when {
          _.IClickOnAnEvent
        }
        .then {
          _.ISeeTheEventDetails
        }
      }

      scenarioWeb("5. Event details are the same as on the event provider", EventDetailTest) {
        implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAFriend
        }
        .when {
          _.IClickOnAnEvent
        }
        .then {
          _.TheDetailsAreTheSameAsOnTheEventProvider
        }
      }
    }

    feature("Require login for purchase") {
      /*
       In order to purchase a ticket
       As a user
       I need to be logged in
       */
      scenarioWeb("6. Logged in user can purchase a ticket", EventTicketPurchase) {
        implicit driver =>
        given {
          MembershipSteps().IAmLoggedInAsAFriend
        }
        .when {
          _.IClickThePurchaseButton
        }
        .then {
          _.ICanPurchaseATicket
        }
      }

      // buy button is disabled for non members
//    scenarioWeb("7. Non logged in user has to login in order to purchase a ticket") {
//      implicit driver =>
//      given {
//        MembershipSteps().IAmNotLoggedIn
//      }
//      .when {
//        _.IClickThePurchaseButton
//      }
//      .then {
//        _.IAmRedirectedToTheChooseTierPage
//      }
////    }
//
//    scenarioWeb("27. Non-registered user can become a friend and purchase a ticket") {
//      implicit driver =>
//      given {
//        MembershipSteps().IAmNotLoggedIn
//      }
//      .when {
//        _.IClickThePurchaseButton
//      }
//      .then {
//        _.IAmRedirectedToTheChooseTierPage
//        .ICanBecomeAFriend
//        .ICanSeeTheTicketIframe
//      }
//    }
//
//    scenarioWeb("38. Non-registered user can become a partner and purchase a ticket") {
//      implicit driver =>
//        given {
//          MembershipSteps().IAmNotLoggedIn
//        }
//          .when {
//          _.IClickThePurchaseButton
//        }
//          .then {
//          _.IAmRedirectedToTheChooseTierPage
//            .ICanBecomeAPartner
//            .ICanSeeTheTicketIframe
//        }
//    }
  }
}
