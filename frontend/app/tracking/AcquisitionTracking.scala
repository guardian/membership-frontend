package tracking

import actions.ActionRefiners.SubReqWithSub
import cats.data.EitherT
import com.gu.acquisition.model.{AcquisitionSubmission, ReferrerAcquisitionData}
import com.gu.acquisition.model.errors.OphanServiceError
import com.gu.acquisition.services.{MockOphanService, OphanService}
import com.gu.memsub.PaymentMethod
import com.gu.okhttp.RequestRunners
import com.gu.salesforce.Tier
import com.gu.monitoring.SafeLogger
import model.MembershipAcquisitionData
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Session}
import utils.TestUsers
import views.support.ThankyouSummary

import scala.concurrent.{ExecutionContext, Future}

trait AcquisitionTracking {

  private def ophanService(isTestUser: Boolean): OphanService = {
    if (isTestUser) MockOphanService
    else OphanService(OphanService.prodEndpoint)(RequestRunners.client)
  }

  private[tracking] def decodeAcquisitionData(session: Session): Option[ReferrerAcquisitionData] = {
    import actions.CommonActions._
    import cats.syntax.either._

    val decodeAttempt: Either[String, ReferrerAcquisitionData] = for {
      json <- Either.fromOption(session.get(acquisitionDataSessionKey), "Session does not contain acquisitionData")
      jsValue <- Either.catchNonFatal(Json.parse(json)).leftMap(_.getMessage)
      acquisitionData <- Json.fromJson[ReferrerAcquisitionData](jsValue).fold(
        errs => Left(s"Unable to decode JSON $jsValue to an instance of ReferrerAcquisitionData. ${JsError.toJson(errs)}"),
        acquisitionData => Right(acquisitionData)
      )
    } yield acquisitionData

    decodeAttempt.leftMap(error => SafeLogger.warn(error)).toOption
  }

  def trackAcquisition(
    summary: ThankyouSummary,
    paymentMethod: Option[PaymentMethod],
    tier: Tier,
    request: SubReqWithSub[_]
  )(implicit ec: ExecutionContext): EitherT[Future, OphanServiceError, AcquisitionSubmission] = {

    def cookieValue(name: String): Option[String] =
      request.cookies.get(name).map(_.value)

    ophanService(TestUsers.isTestUser(request.user.user)).submit(MembershipAcquisitionData(
      summary.amountPaidToday,
      summary.billingPeriod,
      paymentMethod,
      tier,
      request.subscriber.contact.mailingCountryParsed.map(_.alpha2),
      request.session.get("pageviewId"),
      cookieValue("vsid"),
      cookieValue("bwid"),
      decodeAcquisitionData(request.session)
    ))
  }
}
