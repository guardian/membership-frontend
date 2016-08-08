package services

import com.gu.i18n.Country._
import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.gu.stripe.Stripe.Customer
import forms.MemberForm.JoinForm
import model.GenericSFContact
import monitoring.MemberMetrics
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.FrontendMemberRepository._
import scala.concurrent.Future
import scala.concurrent.duration._
import dispatch._, Defaults.timer

object FrontendMemberRepository {
  type UserId = String
}

class SalesforceService(salesforceConfig: SalesforceConfig) extends api.SalesforceService {

  private implicit val system = Akka.system

  val metricsVal = new MemberMetrics(salesforceConfig.envName)

  private val repository = new SimpleContactRepository(salesforceConfig, system.scheduler, "Frontend")

  override def getMember(userId: UserId): Future[Option[GenericSFContact]] =
    repository.get(userId)

  override def metrics = metricsVal

  override def upsert(user: IdUser, joinData: JoinForm): Future[ContactId] =
    upsert(user.id, initialData(user, joinData))

  override def updateMemberStatus(user: IdMinimalUser, tier: Tier, customer: Option[Customer]): Future[ContactId] =
    upsert(user.id, memberData(tier, customer))

  private def memberData(tier: Tier, customerOpt: Option[Customer]): JsObject = Json.obj(
    Keys.TIER -> tier.name
  ) ++ customerOpt.map { customer =>
    Json.obj(
      Keys.STRIPE_CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Json.obj())

  private def initialData(user: IdUser, formData: JoinForm): JsObject = {
    Seq(Json.obj(
      Keys.EMAIL -> user.primaryEmailAddress,
      Keys.FIRST_NAME -> formData.name.first,
      Keys.LAST_NAME -> formData.name.last,
      Keys.MAILING_STREET -> formData.deliveryAddress.line,
      Keys.MAILING_CITY -> formData.deliveryAddress.town,
      Keys.MAILING_STATE -> formData.deliveryAddress.countyOrState,
      Keys.MAILING_POSTCODE -> formData.deliveryAddress.postCode,
      Keys.MAILING_COUNTRY -> formData.deliveryAddress.country.getOrElse(UK).alpha2,
      Keys.ALLOW_MEMBERSHIP_MAIL -> true
    )) ++ Map(
      Keys.ALLOW_THIRD_PARTY_EMAIL -> formData.marketingChoices.thirdParty,
      Keys.ALLOW_GU_RELATED_MAIL -> formData.marketingChoices.gnm
    ).collect { case (k, Some(v)) => Json.obj(k -> v) }
  }.reduce(_ ++ _)

  private def upsert(userId: UserId, value: JsObject) =
  // upsert is POST request but safe to retry
  retry.Backoff(max = 2, delay = 2.seconds, base = 2) { () =>
    repository.upsert(Some(userId), value).either
  }.map {
    case Left(e) => throw new SalesforceServiceError(s"User $userId could not be upsert in Salesforce", e)
    case Right(contactId) => contactId
  }
}

case class SalesforceServiceError(msg: String, cause: Throwable) extends Throwable(msg, cause)
