package services

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Null, Elem}
import akka.agent.Agent

import com.gu.membership.salesforce.Tier

import org.joda.time.DateTime

import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.concurrent.Akka

import configuration.Config
import model.Zuora._
import model.ZuoraObject
import model.Stripe

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait ZuoraService {
  val apiUrl: String
  val apiUsername: String
  val apiPassword: String

  def authentication: Authentication

  private def soapBuilder(body: Elem, head: Option[Elem] = None): String = {
    val xml =
      <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.zuora.com/"
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:ns1="http://api.zuora.com/"
                        xmlns:ns2="http://object.api.zuora.com/">
        <soapenv:Header>{head.getOrElse(Null)}</soapenv:Header>
        <soapenv:Body>{body}</soapenv:Body>
      </soapenv:Envelope>

    // Must toString() because Zuora does not like Content-Type
    xml.toString()
  }

  def request(body: Elem): Future[Elem] = {
    val head =
      <ns1:SessionHeader>
        <ns1:session>{authentication.token}</ns1:session>
      </ns1:SessionHeader>


    val xml = soapBuilder(body, Some(head))

    Logger.debug("ZuoraSOAP authenticated request")
    Logger.debug(xml)

    WS.url(authentication.url).post(xml).map { result =>
      Logger.debug(s"Got result ${result.status}")
      Logger.debug(result.body)
      result.xml
    }
  }

  def getAuthentication: Future[Authentication] = {
    val xml = soapBuilder(ZuoraObject.login(apiUsername, apiPassword))
    WS.url(apiUrl).post(xml).map(result => Authentication(result.xml))
  }
}

trait SubscriptionService {
  val zuora: ZuoraService

  val friendPlan = "2c92c0f945fee1c90146057402c7066b"

  case class PaidPlan(monthly: String, annual: String)

  object PaidPlan {
    val plans = Map(
      Tier.Partner -> PaidPlan("2c92c0f945fee1c9014605749e450969", "2c92c0f8471e22bb01471ffe9596366c"),
      Tier.Patron -> PaidPlan("2c92c0f845fed48301460578277167c3", "2c92c0f9471e145d01471ffd7c304df9")
    )

    def apply(tier: Tier.Tier, annual: Boolean): String = {
      val plan = plans(tier)
      if (annual) plan.annual else plan.monthly
    }
  }

  /**
   * Zuora doesn't return fields which are empty! This method guarantees that the keys will
   * be in the map and also that the query only returns one result
   */
  def queryOne(fields: Seq[String], table: String, where: String): Future[Map[String, String]] = {
    val q = s"SELECT ${fields.mkString(",")} FROM $table WHERE $where"
    zuora.request(ZuoraObject.query(q)).map(Query(_)).map { case Query(results) =>
      if (results.length != 1) {
        throw new SubscriptionServiceError(s"Query $q returned more than one result")
      }

      fields.map { field => (field, results(0).getOrElse(field, "")) }.toMap
    }
  }

  def queryOne(field: String, table: String, where: String): Future[String] =
    queryOne(Seq(field), table, where).map(_(field))

  def createPaidSubscription(sfAccountId: String, customer: Stripe.Customer, tier: Tier.Tier,
                             annual: Boolean): Future[Subscription] = {
    val plan = PaidPlan(tier, annual)
    zuora.request(ZuoraObject.subscribe(sfAccountId, Some(customer), plan)).map(Subscription(_))
  }

  def createFriendSubscription(sfAccountId: String): Future[Subscription] = {
    zuora.request(ZuoraObject.subscribe(sfAccountId, None, friendPlan)).map(Subscription(_))
  }

  def getInvoiceSummary(sfAccountId: String): Future[InvoiceItem] = {
    val invoiceKeys = Seq("ServiceStartDate", "ServiceEndDate", "ProductName", "ChargeAmount", "TaxAmount")
    for {
      accountId <- queryOne("Id", "Account", s"crmId='$sfAccountId'")
      subscriptionId <- queryOne("Id", "Subscription", s"AccountId='$accountId'")
      invoice <- queryOne(invoiceKeys, "InvoiceItem", s"SubscriptionId='$subscriptionId'")
    } yield {
      val startDate = new DateTime(invoice("ServiceStartDate"))
      val endDate = new DateTime(invoice("ServiceEndDate")).plusDays(1) // Yes we really have to +1 day
      val planAmount = invoice("ChargeAmount").toFloat + invoice("TaxAmount").toFloat

      InvoiceItem(invoice("ProductName"), planAmount, startDate, endDate)
    }
  }

}

object SubscriptionService extends SubscriptionService {
  val zuora = new ZuoraService {
    val apiUrl = Config.zuoraApiUrl
    val apiUsername = Config.zuoraApiUsername
    val apiPassword = Config.zuoraApiPassword

    def authentication: Authentication = authenticationAgent.get()
  }

  private implicit val system = Akka.system

  val authenticationAgent = Agent[Authentication](Authentication("", ""))

  def refresh() {
    Logger.debug("Refreshing Zuora login")
    authenticationAgent.sendOff(_ => {
      val auth = Await.result(zuora.getAuthentication, 15.seconds)
      Logger.debug(s"Got Zuora login $auth")
      auth
    })
  }

  def start() {
    Logger.info("Starting Zuora background tasks")
    system.scheduler.schedule(0.seconds, 2.hours) { refresh() }
  }
}
