package services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Null, Elem, PrettyPrinter}

import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat

import play.api.Logger
import play.api.libs.ws.WS
import play.api.Play.current

import forms.MemberForm.{NameForm, AddressForm}
import model.{Zuora, Stripe}
import model.Zuora._
import model.ZuoraDeserializer._

case class ZuoraServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object ZuoraServiceHelpers {
  def formatDateTime(dt: DateTime): String = {
    val str = ISODateTimeFormat.dateTime().print(dt.withZone(DateTimeZone.UTC))
    // Zuora doesn't accept Z for timezone
    str.replace("Z", "+00:00")
  }
}

trait ZuoraService {
  import ZuoraServiceHelpers._

  val apiUrl: String
  val apiUsername: String
  val apiPassword: String

  def authentication: Authentication

  trait ZuoraAction[T <: ZuoraObject] {
    val body: Elem

    val authRequired = true
    val singleTransaction = false

    // The .toString is necessary because Zuora doesn't like Content-Type application/xml
    // which Play automatically adds if you pass it Elems
    lazy val xml = {
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

    def mkRequest()(implicit reader: ZuoraReader[T]): Future[T] = {
      val url = if (authRequired) authentication.url else apiUrl

      if (authRequired && authentication.url.length == 0) {
        throw ZuoraServiceError(s"Can't build authenticated request for ${getClass.getSimpleName}, no Zuora authentication")
      }

      Logger.debug(s"Zuora action ${getClass.getSimpleName}")
      Logger.debug(xml)

      WS.url(url).post(xml).map { result =>
        Logger.debug(s"Got result ${result.status}")
        Logger.debug(new PrettyPrinter(70, 2).format(result.xml))
        // TODO: check result for generic errors
        reader.read(result.xml)
      }
    }
  }

  case class CreatePaymentMethod(accountId: String, customer: Stripe.Customer) extends ZuoraAction[PaymentMethod] {
    val body =
      <ns1:create>
        <ns1:zObjects xsi:type="ns2:PaymentMethod">
          <ns2:AccountId>{accountId}</ns2:AccountId>
          <ns2:TokenId>{customer.card.id}</ns2:TokenId>
          <ns2:SecondTokenId>{customer.id}</ns2:SecondTokenId>
          <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
        </ns1:zObjects>
      </ns1:create>
  }

  // TODO: create model and reader
  case class SetDefaultPaymentMethod(accountId: String, paymentMethodId: String) extends ZuoraAction[Subscription] {
    val body =
      <ns1:update>
        <ns1:zObjects xsi:type="ns2:Account">
          <ns2:Id>{accountId}</ns2:Id>
          <ns2:DefaultPaymentMethodId>{paymentMethodId}</ns2:DefaultPaymentMethodId>
        </ns1:zObjects>
      </ns1:update>
  }

  case class Login() extends ZuoraAction[Authentication] {
    override val authRequired = false

    val body =
      <api:login>
        <api:username>{apiUsername}</api:username>
        <api:password>{apiPassword}</api:password>
      </api:login>
  }

  case class Query(query: String) extends ZuoraAction[Zuora.Query] {
    val body =
      <ns1:query>
        <ns1:queryString>{query}</ns1:queryString>
      </ns1:query>
  }

  case class Subscribe(sfAccountId: String, sfContactId: String, customerOpt: Option[Stripe.Customer],
                       ratePlanId: String, name: NameForm, address: AddressForm) extends ZuoraAction[Subscription] {

    val body = {
      val now = formatDateTime(DateTime.now)

      val payment = customerOpt.map { customer =>
        <ns1:PaymentMethod xsi:type="ns2:PaymentMethod">
          <ns2:TokenId>{customer.card.id}</ns2:TokenId>
          <ns2:SecondTokenId>{customer.id}</ns2:SecondTokenId>
          <ns2:Type>CreditCardReferenceTransaction</ns2:Type>
        </ns1:PaymentMethod>
      }.getOrElse(Null)

      // We can't use Zuora's Address2 field because Salesforce doesn't have an Address2 field which
      // means that when SF address changes are pushed to Zuora, Address2 would not be updated.
      val addressLine = Seq(address.lineOne, address.lineTwo).filter(_.nonEmpty).mkString(", ")

      // NOTE: This appears to be white-space senstive in some way. Zuora rejected
      // the XML after Intellij auto-reformatted the code.
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
            <ns2:sfContactId__c>{sfContactId}</ns2:sfContactId__c>
          </ns1:Account>
          {payment}
          <ns1:BillToContact xsi:type="ns2:Contact">
            <ns2:FirstName>{name.first}</ns2:FirstName>
            <ns2:LastName>{name.last}</ns2:LastName>
            <ns2:Address1>{addressLine}</ns2:Address1>
            <ns2:City>{address.town}</ns2:City>
            <ns2:PostalCode>{address.postCode}</ns2:PostalCode>
            <ns2:State>{address.countyOrState}</ns2:State>
            <ns2:Country>{address.country}</ns2:Country>
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
    extends ZuoraAction[Subscription] {

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

  // TODO: make amendment model and reader
  case class DowngradePlan(subscriptionId: String, subscriptionRatePlanId: String, newRatePlanId: String,
                           date: DateTime) extends ZuoraAction[Subscription] {

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

  // TODO: make amendment model and reader
  case class UpgradePlan(subscriptionId: String, subscriptionRatePlanId: String, newRatePlanId: String)
    extends ZuoraAction[Subscription] {

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

  def queryOne(fields: Seq[String], table: String, where: String): Future[Map[String, String]] = {
    val q = s"SELECT ${fields.mkString(",")} FROM $table WHERE $where"
    Query(q).mkRequest().map { case Zuora.Query(results) =>
      if (results.length != 1) {
        throw new SubscriptionServiceError(s"Query $q returned ${results.length} results, expected one")
      }

      results(0)
    }
  }

  def queryOne(field: String, table: String, where: String): Future[String] =
    queryOne(Seq(field), table, where).map(_(field))

}
