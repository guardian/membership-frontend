package com.gu.salesforce

import org.specs2.mutable._
import play.api.libs.json.{JsError, JsSuccess}
import utils.Resource

class SFContactRecordTest extends Specification {

  "SFContactRecord" should {
    "read successful Salesforce response" in {
      val result = SFContactRecord.readResponse(Resource.getJson("salesforce/contact-upsert.response.good.json"))

      result must beLike {
        case JsSuccess(contact: SFContactRecord, _) if contact.Id == "0031100000csfTPAAY" => ok
      }
    }

    "read an error response from Salesforce" in {
      val result = SFContactRecord.readResponse(Resource.getJson("salesforce/contact-upsert.response.error.json"))
      result must beLike {
        case JsError(t) => ok
      }
    }
  }
}
