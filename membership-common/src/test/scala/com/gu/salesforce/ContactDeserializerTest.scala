package com.gu.salesforce
import com.gu.salesforce.ContactDeserializer._
import org.specs2.mutable._
import utils.Resource

class ContactDeserializerTest extends Specification {

  "MemberDeserializer" should {
    "deserialize FreeMember" in {
      val c = Resource.getJson("free-member.json").as[Contact]
      c.salesforceAccountId mustEqual "000001"
      c.salesforceContactId mustEqual "00000003dfdsf"
      c.identityId must beSome("1004444")
      c.regNumber must beNone
    }

    "deserialize PaidMember" in {
      val c = Resource.getJson("paid-member.json").as[Contact]
      c.salesforceAccountId mustEqual "0011100000XjDmQAAV"
      c.salesforceContactId mustEqual "0031100000WDp1LAAT"
      c.identityId must beSome("10000007")
      c.regNumber must beSome("1234")
    }

    "deserialize digipack subscriber" in {
      val c = Resource.getJson("subs-member.json").as[Contact]
      c.salesforceAccountId mustEqual "123"
      c.salesforceContactId mustEqual "123"
      c.identityId must beSome("ff")
      c.regNumber must beNone
    }

    "deserialize subscriber with no identity id" in {
      val c = Resource.getJson("non-member-contact.json").as[Contact]
      c.salesforceAccountId mustEqual "123"
      c.salesforceContactId mustEqual "321"
      c.identityId must beNone
      c.regNumber must beNone
    }
  }
}
