package com.gu.membership.salesforce

import org.specs2.mutable.Specification
import utils.Resource

import MemberDeserializer._

class MemberDeserializerTest extends Specification {
  "MemberDeserializer" should {
    "deserialize Member" in {
      val resource = Resource.getJson("member.json")
      val member = resource.asOpt[Member]

      member must beSome
      member.get.identityId mustEqual "1004444"
    }
  }
}
