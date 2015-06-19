package tests

import com.gu.membership.tags.OptionalTest
import steps.MembershipSteps

/**
 * Created by jao on 30/09/2014.
 */
class MembershipSubscriptionTests extends BaseMembershipTest {

  feature("A user's information should be pre-populated") {

    scenarioWeb("46. A user's Identity information is pre-populated in the membership subscription form", OptionalTest) {
      implicit driver =>
        given {
          MembershipSteps().IHaveInformationInIdentity
        }
        .when {
          _.IGoToBecomeAPartner
        }
        .then {
          _.TheInformationHasBeenLoadedFromIdentity
        }
    }
  }
}
