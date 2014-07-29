package services

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Null, Elem}
import akka.agent.Agent

import com.gu.membership.salesforce.Tier.Tier

import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.concurrent.Akka

import configuration.Config
import model.Zuora._
import model.Stripe

trait ZuoraSOAP {
  val apiUrl: String
  val apiUsername: String
  val apiPassword: String

  def authentication: Authentication

  private def soapBuilder(body: Elem, head: Option[Elem] = None): String = {
    val xml =
      <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:api="http://api.zuora.com/">
        <soapenv:Header>{head.getOrElse(Null)}</soapenv:Header>
        <soapenv:Body>{body}</soapenv:Body>
      </soapenv:Envelope>

    // Must toString() because Zuora does not like Content-Type
    xml.toString()
  }

  def authRequest(body: Elem): Future[Elem] = {
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
    val xml = soapBuilder(Authentication.login(apiUsername, apiPassword))
    WS.url(apiUrl).post(xml).map(result => Authentication(result.xml))
  }
}

trait SubscriptionService {
  def createSubscription(salesforceContactId: String, customer: Stripe.Customer, tier: Tier): Future[Subscription]
}

object SubscriptionService extends SubscriptionService {
  val zuoraSOAP = new ZuoraSOAP {
    val apiUrl = Config.zuoraApiUrlSOAP

    val apiUsername = Config.zuoraApiUsername
    val apiPassword = Config.zuoraApiPassword

    def authentication: Authentication = authenticationAgent.get()
  }

  def createSubscription(salesforceContactId: String, customer: Stripe.Customer, tier: Tier): Future[Subscription] = {
    zuoraSOAP.authRequest(Subscription.subscribe(salesforceContactId, customer, tier)).map(Subscription(_))
  }

  private implicit val system = Akka.system

  val authenticationAgent = Agent[Authentication](Authentication("", ""))

  def refresh() {
    Logger.debug("Refreshing Zuora login")
    authenticationAgent.sendOff(_ => {
      val auth = Await.result(zuoraSOAP.getAuthentication, 15.seconds)
      Logger.debug(s"Got Zuora login $auth")
      auth
    })
  }

  def start() {
    Logger.info("Starting Zuora background tasks")
    system.scheduler.schedule(0.seconds, 2.hours) { refresh() }
  }
}
