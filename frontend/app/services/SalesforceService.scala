package services

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.salesforce.ContactDeserializer.Keys
import com.gu.salesforce._
import com.gu.stripe.Stripe.Customer
import dispatch.Defaults.timer
import dispatch._
import forms.MemberForm.{CommonForm, JoinForm, MonthlyContributorForm}
import model.GenericSFContact
import monitoring.{ContributorMetrics, MemberMetrics}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.FrontendMemberRepository._

import scala.concurrent.Future
import scala.concurrent.duration._

object FrontendMemberRepository {
  type UserId = String
}

class SalesforceService(salesforceConfig: SalesforceConfig) extends api.SalesforceService {

  private implicit val system = Akka.system

  val metricsVal = new MemberMetrics(salesforceConfig.envName)

  val contributorMetricsVal = new ContributorMetrics(salesforceConfig.envName)

  private val repository = new SimpleContactRepository(salesforceConfig, system.scheduler, "membership")

  override def getMember(userId: UserId): Future[Option[GenericSFContact]] =
    repository.get(userId)

  override def metrics = metricsVal

  override def contributorMetrics = contributorMetricsVal

  override def upsert(user: IdUser, joinData: CommonForm): Future[ContactId] =
    upsert(user.id, initialData(user, joinData))

  override def updateMemberStatus(user: IdMinimalUser, tier: Tier, customer: Option[Customer]): Future[ContactId] =
    upsert(user.id, memberData(tier, customer))

  override def isAuthenticated = repository.salesforce.isAuthenticated

  private def memberData(tier: Tier, customerOpt: Option[Customer]): JsObject = Json.obj(
    Keys.TIER -> tier.name
  ) ++ customerOpt.map { customer =>
    Json.obj(
      Keys.STRIPE_CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Json.obj())

  private def initialData(user: IdUser, formData: CommonForm): JsObject = {
    formData match {
      case jf : JoinForm => Seq(Json.obj(
        Keys.EMAIL -> user.primaryEmailAddress,
        Keys.FIRST_NAME -> jf.name.first,
        Keys.LAST_NAME -> jf.name.last,
        Keys.MAILING_STREET -> jf.deliveryAddress.line,
        Keys.MAILING_CITY -> jf.deliveryAddress.town,
        Keys.MAILING_STATE -> jf.deliveryAddress.countyOrState,
        Keys.MAILING_POSTCODE -> jf.deliveryAddress.postCode,
        Keys.MAILING_COUNTRY -> jf.deliveryAddress.country.fold(jf.deliveryAddress.countryName)(_.name),
        Keys.ALLOW_MEMBERSHIP_MAIL -> true
      )) ++ Map(
        Keys.ALLOW_THIRD_PARTY_EMAIL -> formData.marketingChoices.thirdParty,
        Keys.ALLOW_GU_RELATED_MAIL -> formData.marketingChoices.gnm
      ).collect { case (k, Some(v)) => Json.obj(k -> v) }

      case _ => Seq(Json.obj(
        Keys.EMAIL -> user.primaryEmailAddress,
        Keys.FIRST_NAME -> formData.name.first,
        Keys.LAST_NAME -> formData.name.last
      )) ++ emailProps(formData)
      }
  }.reduce(_ ++ _)

  private def emailProps(formData: CommonForm) = {
    val allowMail = !formData.isInstanceOf[MonthlyContributorForm]
    Seq(Json.obj(
      Keys.ALLOW_MEMBERSHIP_MAIL -> allowMail,
      Keys.ALLOW_THIRD_PARTY_EMAIL -> (allowMail && formData.marketingChoices.thirdParty.getOrElse(false)),
      Keys.ALLOW_GU_RELATED_MAIL -> (allowMail && formData.marketingChoices.gnm.getOrElse(false))
    ))
  }

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
