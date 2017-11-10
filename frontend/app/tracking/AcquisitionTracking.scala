package tracking

import cats.data.EitherT
import com.gu.acquisition.model.{AcquisitionSubmission, ReferrerAcquisitionData}
import com.gu.acquisition.model.errors.OphanServiceError
import com.gu.acquisition.services.{MockOphanService, OphanService}
import com.gu.okhttp.RequestRunners
import com.gu.salesforce.Tier
import com.gu.zuora.soap.models.Commands
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.JoinForm
import model.MembershipAcquisitionData
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{Request, Session}
import views.support.ThankyouSummary

import scala.concurrent.{ExecutionContext, Future}

trait AcquisitionTracking extends LazyLogging {

  private def ophanService(isTestUser: Boolean): OphanService = {
    if (isTestUser) MockOphanService
    else OphanService(OphanService.prodEndpoint)(RequestRunners.client)
  }

  private def decodeAcquisitionData(session: Session): Option[ReferrerAcquisitionData] = {
    import ReferrerAcquisitionData._
    import cats.syntax.either._

    val decodeAttempt: Either[String, ReferrerAcquisitionData] = for {
      json <- Either.fromOption(session.get("acquisitionData"), "Session does not contain acquisitionData")
      jsValue <- Either.catchNonFatal(Json.parse(json)).leftMap(_.getMessage)
      acquisitionData <- Json.fromJson(jsValue).fold(
        errs => Left(s"Unable to decode JSON $jsValue to an instance of ReferrerAcquisitionData. ${JsError.toJson(errs)}"),
        acquisitionData => Right(acquisitionData)
      )
    } yield acquisitionData

    decodeAttempt.leftMap(error => logger.warn(error)).toOption
  }

  def trackAcquisition(
    summary: ThankyouSummary,
    paymentMethod: Option[Commands.PaymentMethod],
    form: JoinForm,
    tier: Tier,
    request: Request[_],
    isTestUser: Boolean
  )(implicit ec: ExecutionContext): EitherT[Future, OphanServiceError, AcquisitionSubmission] = {

    def cookieValue(name: String): Option[String] =
      request.cookies.get(name).map(_.value)

    ophanService(isTestUser).submit(MembershipAcquisitionData(
      summary.amountPaidToday,
      summary.billingPeriod,
      paymentMethod,
      form,
      tier,
      cookieValue("vsid"),
      cookieValue("bwid"),
      decodeAcquisitionData(request.session)
    ))
  }
}
