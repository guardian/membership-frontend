package com.gu.zuora.soap

import Readers._
import com.gu.zuora.soap.models.Results.UpdateResult
import com.gu.zuora.soap.models.errors.{ZuoraPartialError, InvalidValue}
import org.scalatest.matchers.should.Matchers
import org.scalatest.freespec.AnyFreeSpec

class ZuoraDeserializerSpec extends AnyFreeSpec with Matchers {
  "An Update can be deserialized" - {
    "into a valid UpdateResult"in {
      val validResponse =
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <ns1:updateResponse xmlns:ns1="http://api.zuora.com/">
              <ns1:result>
                <ns1:Id>2c92c0f94ed8d0d7014ef90424654cfc</ns1:Id>
                <ns1:Success>true</ns1:Success>
              </ns1:result>
            </ns1:updateResponse>
          </soapenv:Body>
        </soapenv:Envelope>

      updateResultReader.read(validResponse.toString()) should be(Right(UpdateResult("2c92c0f94ed8d0d7014ef90424654cfc")))
    }

    "into a Failure" in {
      val invalidResponse=
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
          <soapenv:Body>
            <ns1:updateResponse xmlns:ns1="http://api.zuora.com/">
              <ns1:result>
                <ns1:Errors>
                  <ns1:Code>INVALID_VALUE</ns1:Code>
                  <ns1:Message>The length of field value is too big.</ns1:Message>
                </ns1:Errors>
                <ns1:Success>false</ns1:Success>
              </ns1:result>
            </ns1:updateResponse>
          </soapenv:Body>
        </soapenv:Envelope>

      val error = ZuoraPartialError(
        "INVALID_VALUE",
        "The length of field value is too big.",
        InvalidValue)

      updateResultReader.read(invalidResponse.toString()) should be(Left(error))
    }
  }
}
