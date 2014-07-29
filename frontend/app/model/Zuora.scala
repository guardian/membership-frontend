package model

import scala.xml.Elem
import org.joda.time.DateTime

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.Tier

object Zuora {
  trait ZuoraObject

  // TODO: add annual plans
  val plans = Map(
    Tier.Friend -> "8a80812a4733a5bb01475f2b6b4c04a2",
    Tier.Partner -> "8a80812a4733a5bb01475f2b6b9204a8",
    Tier.Patron -> "8a80812a4733a5bb01475f2b6ae80498"
  )

  case class Authentication(token: String, url: String) extends ZuoraObject

  case class Subscription() extends ZuoraObject

  object Authentication {
    def login(user: String, pass: String): Elem = {
      <api:login>
        <api:username>{user}</api:username>
        <api:password>{pass}</api:password>
      </api:login>
    }

    def apply(elem: Elem): Authentication = {
      val result = elem \\ "loginResponse" \ "result"
      Authentication((result \ "Session").text, (result \ "ServerUrl").text)
    }
  }

  object Subscription {
    def subscribe(salesforceContactId: String, customer: Stripe.Customer, tier: Tier): Elem = {
      val now = DateTime.now.toString("YYYY-MM-dd'T'HH:mm:ss")

      <ns1:subscribe>
        <ns1:subscribes>
          <ns1:Account xsi:type="ns2:Account">
           <ns2:AutoPay>true</ns2:AutoPay>
           <ns2:BcdSettingOption>AutoSet</ns2:BcdSettingOption>
           <ns2:BillCycleDay>0</ns2:BillCycleDay>
           <ns2:Currency>GBP</ns2:Currency>
           <ns2:Name>Account Name</ns2:Name>
           <ns2:PaymentTerm>Due Upon Receipt</ns2:PaymentTerm>
           <ns2:Batch>Batch1</ns2:Batch>
           <ns2:CrmId>{salesforceContactId}</ns2:CrmId>
          </ns1:Account>
          <ns1:PaymentMethod xsi:type="ns2:PaymentMethod">
            <ns2:TokenId>{customer.cardOpt.get.id}</ns2:TokenId>
            <ns2:SecondTokenId>{customer.id}</ns2:SecondTokenId>
            <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
          </ns1:PaymentMethod>
          <ns1:BillToContact xsi:type="ns2:Contact">
            <ns2:Address1>25 North Row</ns2:Address1>
            <ns2:City>London</ns2:City>
            <ns2:Country>United Kingdom</ns2:Country>
            <ns2:FirstName>Zuora</ns2:FirstName>
            <ns2:LastName>Customer</ns2:LastName>
            <ns2:PostalCode>W1K 6DJ</ns2:PostalCode>
          </ns1:BillToContact>
          <ns1:PreviewOptions>
            <ns1:EnablePreviewMode>false</ns1:EnablePreviewMode>
            <ns1:NumberOfPeriods>1</ns1:NumberOfPeriods>
          </ns1:PreviewOptions>
          <ns1:SubscribeOptions>
            <ns1:GenerateInvoice>true</ns1:GenerateInvoice>
            <ns1:ProcessPayments>true</ns1:ProcessPayments>
          </ns1:SubscribeOptions>
          <ns1:SubscriptionData>
            <ns1:Subscription xsi:type="ns2:Subscription">
              <ns2:AutoRenew>true</ns2:AutoRenew>
              <ns2:ContractEffectiveDate>{now}</ns2:ContractEffectiveDate>
              <ns2:InitialTerm>12</ns2:InitialTerm>
              <ns2:RenewalTerm>12</ns2:RenewalTerm>
              <ns2:TermStartDate>{now}</ns2:TermStartDate>
              <ns2:TermType>TERMED</ns2:TermType>
            </ns1:Subscription>
            <ns1:RatePlanData>
              <ns1:RatePlan xsi:type="ns2:RatePlan">
                <ns2:ProductRatePlanId>{plans(tier)}</ns2:ProductRatePlanId>
              </ns1:RatePlan>
            </ns1:RatePlanData>
          </ns1:SubscriptionData>
        </ns1:subscribes>
      </ns1:subscribe>
    }

    def apply(elem: Elem): Subscription = {
      Subscription()
    }
  }
}
