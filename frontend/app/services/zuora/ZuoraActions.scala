package services.zuora

import com.gu.membership.salesforce.MemberId
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.Address
import forms.MemberForm.NameForm
import model.Zuora._
import org.joda.time.DateTime
import services.zuora.ZuoraServiceHelpers._

import scala.xml.{Elem, Null}

trait ZuoraAction[T <: ZuoraResult] {
  protected val body: Elem

  val authRequired = true
  val singleTransaction = false

  // The .toString is necessary because Zuora doesn't like Content-Type application/xml
  // which Play automatically adds if you pass it Elems
  def xml(implicit authentication: Authentication) = {
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.zuora.com/"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://api.zuora.com/"
                      xmlns:ns2="http://object.api.zuora.com/">
      <soapenv:Header>
        {if (authRequired) {
        <ns1:SessionHeader>
          <ns1:session>
            {authentication.token}
          </ns1:session>
        </ns1:SessionHeader>
      }}
        {
        if (singleTransaction) {
          <ns1:CallOptions>
            <ns1:useSingleTransaction>true</ns1:useSingleTransaction>
          </ns1:CallOptions>
        }
        }
      </soapenv:Header>
      <soapenv:Body>{body}</soapenv:Body>
    </soapenv:Envelope>.toString()
  }

  def sanitized = body.toString()
}

case class CreatePaymentMethod(account: Account, customer: Stripe.Customer) extends ZuoraAction[CreateResult] {
  val body =
    <ns1:create>
      <ns1:zObjects xsi:type="ns2:PaymentMethod">
        <ns2:AccountId>{account.id}</ns2:AccountId>
        <ns2:TokenId>{customer.card.id}</ns2:TokenId>
        <ns2:SecondTokenId>{customer.id}</ns2:SecondTokenId>
        <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
      </ns1:zObjects>
    </ns1:create>
}

case class EnablePayment(account: Account, paymentMethod: CreateResult) extends ZuoraAction[UpdateResult] {
  val body =
    <ns1:update>
      <ns1:zObjects xsi:type="ns2:Account">
        <ns2:Id>{account.id}</ns2:Id>
        <ns2:DefaultPaymentMethodId>{paymentMethod.id}</ns2:DefaultPaymentMethodId>
        <ns2:AutoPay>true</ns2:AutoPay>
      </ns1:zObjects>
    </ns1:update>
}

case class Login(apiConfig: ZuoraApiConfig) extends ZuoraAction[Authentication] {
  override val authRequired = false

  val body =
    <api:login>
      <api:username>{apiConfig.username}</api:username>
      <api:password>{apiConfig.password}</api:password>
    </api:login>

  override def sanitized = "<api:login>...</api:login>"
}

case class Query(query: String) extends ZuoraAction[QueryResult] {
  val body =
    <ns1:query>
      <ns1:queryString>{query}</ns1:queryString>
    </ns1:query>
}

case class Subscribe(memberId: MemberId, customerOpt: Option[Stripe.Customer], ratePlanId: String, name: NameForm,
                     address: Address) extends ZuoraAction[SubscribeResult] {

  val body = {
    val now = formatDateTime(DateTime.now)

    val payment = customerOpt.map { customer =>
      <ns1:PaymentMethod xsi:type="ns2:PaymentMethod">
        <ns2:TokenId>{customer.card.id}</ns2:TokenId>
        <ns2:SecondTokenId>{customer.id}</ns2:SecondTokenId>
        <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
      </ns1:PaymentMethod>
    }.getOrElse(Null)

    // NOTE: This appears to be white-space senstive in some way. Zuora rejected
    // the XML after Intellij auto-reformatted the code.
    <ns1:subscribe>
      <ns1:subscribes>
        <ns1:Account xsi:type="ns2:Account">
          <ns2:AutoPay>{customerOpt.isDefined}</ns2:AutoPay>
          <ns2:BcdSettingOption>AutoSet</ns2:BcdSettingOption>
          <ns2:BillCycleDay>0</ns2:BillCycleDay>
          <ns2:Currency>GBP</ns2:Currency>
          <ns2:Name>{memberId.salesforceAccountId}</ns2:Name>
          <ns2:PaymentTerm>Due Upon Receipt</ns2:PaymentTerm>
          <ns2:Batch>Batch1</ns2:Batch>
          <ns2:CrmId>{memberId.salesforceAccountId}</ns2:CrmId>
          <ns2:sfContactId__c>{memberId.salesforceContactId}</ns2:sfContactId__c>
        </ns1:Account>
        {payment}
        <ns1:BillToContact xsi:type="ns2:Contact">
          <ns2:FirstName>{name.first}</ns2:FirstName>
          <ns2:LastName>{name.last}</ns2:LastName>
          <ns2:Address1>{address.line}</ns2:Address1>
          <ns2:City>{address.town}</ns2:City>
          <ns2:PostalCode>{address.postCode}</ns2:PostalCode>
          <ns2:State>{address.countyOrState}</ns2:State>
          <ns2:Country>{address.country.alpha2}</ns2:Country>
          <ns2:NickName>{address.country.alpha2}</ns2:NickName>
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

case class CancelPlan(subscriptionId: String, subscriptionRatePlanId: String, date: DateTime)
  extends ZuoraAction[AmendResult] {

  val body = {
    val dateStr = formatDateTime(date)

    <ns1:amend>
      <ns1:requests>
        <ns1:Amendments>
          <ns2:EffectiveDate>{dateStr}</ns2:EffectiveDate>
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
          <ns2:Name>Cancellation</ns2:Name>
          <ns2:RatePlanData>
            <ns1:RatePlan>
              <ns2:AmendmentSubscriptionRatePlanId>{subscriptionRatePlanId}</ns2:AmendmentSubscriptionRatePlanId>
            </ns1:RatePlan>
          </ns2:RatePlanData>
          <ns2:ServiceActivationDate/>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{subscriptionId}</ns2:SubscriptionId>
          <ns2:Type>Cancellation</ns2:Type>
        </ns1:Amendments>
      </ns1:requests>
    </ns1:amend>
  }
}

case class DowngradePlan(subscriptionId: String, subscriptionRatePlanId: String, newRatePlanId: String,
                         date: DateTime) extends ZuoraAction[AmendResult] {

  override val singleTransaction = true

  val body = {
    val dateStr = formatDateTime(date)

    <ns1:amend>
      <ns1:requests>
        <ns1:Amendments>
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
          <ns2:Name>Downgrade</ns2:Name>
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
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
          <ns2:Name>Downgrade</ns2:Name>
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
          <ns1:GenerateInvoice>false</ns1:GenerateInvoice>
          <ns1:ProcessPayments>false</ns1:ProcessPayments>
        </ns1:AmendOptions>
        <ns1:PreviewOptions>
          <ns1:EnablePreviewMode>false</ns1:EnablePreviewMode>
          <ns1:PreviewThroughTermEnd>true</ns1:PreviewThroughTermEnd>
        </ns1:PreviewOptions>
      </ns1:requests>
    </ns1:amend>
  }
}

case class UpgradePlan(subscriptionId: String, subscriptionRatePlanId: String, newRatePlanId: String)
  extends ZuoraAction[AmendResult] {

  override val singleTransaction = true

  val body = {
    val dateStr = formatDateTime(DateTime.now)

    <ns1:amend>
      <ns1:requests>
        <ns1:Amendments>
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
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
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
          <ns2:EffectiveDate>{dateStr}</ns2:EffectiveDate>
          <ns2:Name>Upgrade</ns2:Name>
          <ns2:Status>Completed</ns2:Status>
          <ns2:SubscriptionId>{subscriptionId}</ns2:SubscriptionId>
          <ns2:TermStartDate>{dateStr}</ns2:TermStartDate>
          <ns2:Type>TermsAndConditions</ns2:Type>
        </ns1:Amendments>
        <ns1:Amendments>
          <ns2:ContractEffectiveDate>{dateStr}</ns2:ContractEffectiveDate>
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
            <ns1:InvoiceTargetDate>{dateStr}</ns1:InvoiceTargetDate>
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
}
