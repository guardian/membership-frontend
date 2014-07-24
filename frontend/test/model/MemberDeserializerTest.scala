package model

import org.specs2.mutable.Specification

import model.MemberDeserializer._
import utils.Resource

class MemberDeserializerTest extends Specification {
  "MemberDeserializer" should {
    "deserialize Member" in {
      val resource = Resource.getJson("model/salesforce/member.json")
      val member = resource.asOpt[Member]

      member must beSome
      member.get.identityId mustEqual "1004444"
    }
  }
}
