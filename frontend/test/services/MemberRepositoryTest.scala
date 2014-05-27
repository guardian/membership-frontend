package services

import play.api.test.PlaySpecification
import model.Tier

class MemberRepositoryTest extends PlaySpecification {

  "MemberRepository" should {

    "put a member tier in the repository" in {
      val members: MemberRepository = AwsMemberTable

      members.putTier("user123", Tier.Partner)
      members.getTier("user123") must equalTo(Tier.Partner)
    }

  }

}
