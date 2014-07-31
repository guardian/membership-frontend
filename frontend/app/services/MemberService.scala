package services

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent

import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka

import com.gu.membership.salesforce._
import com.gu.membership.salesforce.Tier.Tier

import com.gu.identity.model.User

import configuration.Config
import model.Eventbrite.{EBEvent, EBDiscount}
import model.Stripe.{Customer, Subscription}

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait MemberService {
  def createMember(user: User, tier: Tier, paymentToken: Option[String]): Future[String] = {
    for {
      customer <- StripeService.Customer.create(user.getPrimaryEmailAddress, paymentToken.get)
      salesforceContactId <- MemberRepository.upsert(user, customer.id, tier)
      subscription <- SubscriptionService.createSubscription("", customer, tier)
    } yield ""
  }

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]] = {

    def createDiscountFor(memberOpt: Option[Member]): Option[Future[EBDiscount]] = {
      // code should be unique for each user/event combination
      memberOpt
        .filter(_.tier >= Tier.Partner)
        .map { member =>
          EventbriteService.createOrGetDiscount(event.id, DiscountCode.generate(s"${member.identityId}_${event.id}"))
        }
    }

    for {
      member <- MemberRepository.get(userId)
      discount <- Future.sequence(createDiscountFor(member).toSeq)
    } yield discount.headOption
  }

  def cancelAnySubscriptionPayment(member: Member): Future[Option[Subscription]] = {
    def cancelSubscription(customer: Customer): Option[Future[Subscription]] = {
      for {
        paymentDetails <- customer.paymentDetails
      } yield {
        StripeService.Subscription.delete(customer.id, paymentDetails.subscription.id)
      }
    }

    for {
      customer <- StripeService.Customer.read(member.stripeCustomerId.get)
      cancelledOpt = cancelSubscription(customer)
      cancelledSubscription <- Future.sequence(cancelledOpt.toSeq)
    } yield cancelledSubscription.headOption
  }

}

object MemberService extends MemberService

object MemberRepository extends MemberRepository {
  val salesforce = new Scalaforce {
    val consumerKey = Config.salesforceConsumerKey
    val consumerSecret = Config.salesforceConsumerSecret

    val apiURL = Config.salesforceApiUrl
    val apiUsername = Config.salesforceApiUsername
    val apiPassword = Config.salesforceApiPassword
    val apiToken = Config.salesforceApiToken

    def authentication: Authentication = authenticationAgent.get()
  }

  private implicit val system = Akka.system

  val authenticationAgent = Agent[Authentication](Authentication("", ""))

  def refresh() {
    Logger.debug("Refreshing Scalaforce login")
    authenticationAgent.sendOff(_ => {
      val auth = Await.result(salesforce.getAuthentication, 15.seconds)
      Logger.debug(s"Got Scalaforce login $auth")
      auth
    })
  }

  def start() {
    Logger.info("Starting Scalaforce background tasks")
    system.scheduler.schedule(0.seconds, 2.hours) { refresh() }
  }
}
