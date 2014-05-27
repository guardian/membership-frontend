package services

import play.api.test.PlaySpecification

class MemberRepositoryTest extends PlaySpecification {

  "MemberRepository" should {

    "put a member tier in the repository" in {
      val members: MemberRepository = AwsMemberTable

      members.putTier("user123", "member")
      members.getTier("user123").get must equalTo("member")
    }.pendingUntilFixed("Ignoring until we set up configs - IKenna")

  }

}
