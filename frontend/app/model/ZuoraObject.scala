package model

import scala.xml.{Null, Elem}
import com.gu.membership.salesforce.Tier.Tier
import org.joda.time.DateTime

object ZuoraObject {

  def amend(subscriptionId: String, subscriptionRatePlanId: String, newRatePlanId: String): Elem = {
    val now = DateTime.now.toString("YYYY-MM-dd'T'HH:mm:ss")

    <ns1:amend>
      <ns1:requests>
        <ns1:Amendments>
          <ns2:ContractEffectiveDate>{now}</ns2:ContractEffectiveDate>
          <ns2:Name>Upgrade</ns2:Name>
          <ns2:RatePlanData>
            <ns1:RatePlan>
              <ns2:AmendmentSubscriptionRatePlanId>{subscriptionRatePlanId}</ns2:AmendmentSubscriptionRatePlanId>
            </ns1:RatePlan>
          </ns2:RatePlanData>
          <ns2:ServiceActivationDate/>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{subscriptionId}</ns2:SubscriptionId>
          <ns2:Type>RemoveProduct</ns2:Type>
        </ns1:Amendments>
        <ns1:Amendments>
          <ns2:ContractEffectiveDate>{now}</ns2:ContractEffectiveDate>
          <ns2:Name>Upgrade</ns2:Name>
          <ns2:RatePlanData>
            <ns1:RatePlan>
              <ns2:ProductRatePlanId>{newRatePlanId}</ns2:ProductRatePlanId>
            </ns1:RatePlan>
          </ns2:RatePlanData>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{subscriptionId}</ns2:SubscriptionId>
          <ns2:Type>NewProduct</ns2:Type>
        </ns1:Amendments>
        <ns1:AmendOptions>
          <ns1:GenerateInvoice>true</ns1:GenerateInvoice>
          <ns1:InvoiceProcessingOptions>
            <ns1:InvoiceTargetDate>{now}</ns1:InvoiceTargetDate>
          </ns1:InvoiceProcessingOptions>
          <ns1:ProcessPayments>true</ns1:ProcessPayments>
        </ns1:AmendOptions>
        <ns1:PreviewOptions>
          <ns1:EnablePreviewMode>false</ns1:EnablePreviewMode>
          <ns1:PreviewThroughTermEnd>true</ns1:PreviewThroughTermEnd>
        </ns1:PreviewOptions>
      </ns1:requests>
    </ns1:amend>
  }

  def login(username: String, password: String): Elem = {
    <api:login>
      <api:username>{username}</api:username>
      <api:password>{password}</api:password>
    </api:login>
  }

  def query(q: String): Elem = {
    <ns1:query>
      <ns1:queryString>{q}</ns1:queryString>
    </ns1:query>
  }

  def subscribe(sfAccountId: String, customerOpt: Option[Stripe.Customer], ratePlanId: String): Elem = {
    val now = DateTime.now.toString("YYYY-MM-dd'T'HH:mm:ss")

    val payment = customerOpt.map { customer =>
      <ns1:PaymentMethod xsi:type="ns2:PaymentMethod">
        <ns2:TokenId>{customer.card.id}</ns2:TokenId>
        <ns2:SecondTokenId>{customer.id}</ns2:SecondTokenId>
        <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
      </ns1:PaymentMethod>
    }.getOrElse(Null)

    // TODO: customer.cardOpt should always be Some

    <ns1:subscribe>
      <ns1:subscribes>
        <ns1:Account xsi:type="ns2:Account">
          <ns2:AutoPay>{customerOpt.isDefined}</ns2:AutoPay>
          <ns2:BcdSettingOption>AutoSet</ns2:BcdSettingOption>
          <ns2:BillCycleDay>0</ns2:BillCycleDay>
          <ns2:Currency>GBP</ns2:Currency>
          <ns2:Name>{sfAccountId}</ns2:Name>
          <ns2:PaymentTerm>Due Upon Receipt</ns2:PaymentTerm>
          <ns2:Batch>Batch1</ns2:Batch>
          <ns2:CrmId>{sfAccountId}</ns2:CrmId>
        </ns1:Account>
        {payment}
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
              <ns2:ProductRatePlanId>{ratePlanId}</ns2:ProductRatePlanId>
            </ns1:RatePlan>
          </ns1:RatePlanData>
        </ns1:SubscriptionData>
      </ns1:subscribes>
    </ns1:subscribe>
  }


}
