package com.gu.zuora.soap.actions

import com.gu.zuora.soap.models.Results.Authentication
import com.gu.zuora.soap.models.Result

import scala.xml.{Elem, NodeSeq}

trait Action[T <: Result] { self =>

  protected val body: Elem

  val authRequired = true
  val singleTransaction = false
  val enableLogging: Boolean = true

  def logInfo: Map[String, String] = Map("Action" -> self.getClass.getSimpleName)
  def additionalLogInfo: Map[String, String] = Map.empty

  def prettyLogInfo = (logInfo ++ additionalLogInfo).map { case (k, v) => s"  - $k: $v" } .mkString("\n")

  def xml(authentication: Option[Authentication]) = {
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.zuora.com/"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://api.zuora.com/"
                      xmlns:ns2="http://object.api.zuora.com/">
      <soapenv:Header>
        {sessionHeader(authentication)}
        {
        if (singleTransaction) {
          <ns1:CallOptions>
            <ns1:useSingleTransaction>true</ns1:useSingleTransaction>
          </ns1:CallOptions>
        }
        }
      </soapenv:Header>
      <soapenv:Body>{body}</soapenv:Body>
    </soapenv:Envelope>
  }

  def sanitized = body.toString()

  private def sessionHeader(authOpt: Option[Authentication]):NodeSeq =
    authOpt.fold(NodeSeq.Empty) { auth =>
      <ns1:SessionHeader>
        <ns1:session>{auth.token}</ns1:session>
      </ns1:SessionHeader>
    }
}
