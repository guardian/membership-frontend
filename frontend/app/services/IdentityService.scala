package services

import cats.data.EitherT
import cats.instances.all._
import cats.syntax.either._
import com.gu.identity.play._
import com.gu.identity.play.idapi.{CreateIdUser, UpdateIdUser, UserRegistrationResult}
import com.gu.memsub.Address
import com.gu.memsub.util.Timing
import configuration.Config
import controllers.IdentityRequest
import dispatch.Defaults.timer
import dispatch._
import forms.MemberForm._
import monitoring.IdentityApiMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}
import services.IdentityService.DisregardResponseContent
import views.support.IdentityUser

import scala.concurrent.Future
import scala.concurrent.duration._

case class IdentityServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object IdentityService {
  val DisregardResponseContent: (WSResponse => Either[String, Unit]) = resp => Right(Unit)

  def privateFieldsFor(form: CommonForm): PrivateFields = {
    val deliveryOpt = form match {
      case d: HasDeliveryAddress => Some(d.deliveryAddress)
      case _ => None
    }

    privateFieldsFor(
      firstName = Some(form.name.first),
      lastName = Some(form.name.last),
      delivery = deliveryOpt,
      billing = form match {
        case b: HasBillingAddress => b.billingAddress.orElse(deliveryOpt)
        case _ => None
      }
    )
  }

  def privateFieldsFor(
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    delivery: Option[Address] = None,
    billing: Option[Address] = None): PrivateFields = {

    def country(address: Option[Address]) = address.map(a => a.country.fold(a.countryName)(_.name))

    PrivateFields(
      firstName,
      lastName,

      delivery.map(_.lineOne),
      delivery.map(_.lineTwo),
      delivery.map(_.town),
      delivery.map(_.countyOrState),
      delivery.map(_.postCode),
      country(delivery),

      billing.map(_.lineOne),
      billing.map(_.lineTwo),
      billing.map(_.town),
      billing.map(_.countyOrState),
      billing.map(_.postCode),
      country(billing)
    )
  }
}

case class IdentityService(identityApi: IdentityApi) {
  def getIdentityUserView(user: IdMinimalUser, identityRequest: IdentityRequest): Future[IdentityUser] =
    getFullUserDetails(user)(identityRequest)
      .zip(doesUserPasswordExist(identityRequest))
      .map { case (fullUser, doesPasswordExist) =>
        IdentityUser(fullUser, doesPasswordExist)
      }

  def getFullUserDetails(user: IdMinimalUser)(implicit identityRequest: IdentityRequest): Future[IdUser] =
    retry.Backoff(max = 3, delay = 2.seconds, base = 2){ () =>
      identityApi.get(s"user/${user.id}", identityRequest.headers, identityRequest.trackingParameters)
    }.map(_.getOrElse{
      val guCookieExists = identityRequest.headers.exists(_._1 == "X-GU-ID-FOWARDED-SC-GU-U")
      val guTokenExists = identityRequest.headers.exists(_._1 == "Authorization")
      val errContext = s"SC_GU_U=$guCookieExists GU-IdentityToken=$guTokenExists trackingParamters=${identityRequest.trackingParameters.toString}"
      throw IdentityServiceError(s"Couldn't get user's ${user.id} full details. $errContext")
    })

  def doesUserPasswordExist(identityRequest: IdentityRequest): Future[Boolean] =
    identityApi.getUserPasswordExists(identityRequest.headers, identityRequest.trackingParameters)

  def updateUserPassword(password: String)(implicit idReq: IdentityRequest) {
    identityApi.post("/user/password", Some(Json.obj("newPassword" -> password)), idReq.headers, idReq.trackingParameters, "update-user-password")(DisregardResponseContent)
  }

  def updateUserFieldsBasedOnUpgrade(userId: String, addressDetails: AddressDetails)(implicit r: IdentityRequest) {
    updateUser(UpdateIdUser(privateFields = Some(IdentityService.privateFieldsFor(
          delivery = Some(addressDetails.deliveryAddress),
          billing = Some(addressDetails.billingAddress.getOrElse(addressDetails.deliveryAddress))))), userId)(r)
  }

  def updateEmail(user: IdMinimalUser, email: String)(implicit idReq: IdentityRequest): EitherT[Future, String, Unit] =
    updateUser(UpdateIdUser(primaryEmailAddress = Some(email)), user.id)

  def reauthUser(email: String, password: String)(implicit idReq: IdentityRequest): EitherT[Future, String, Unit] = {
    val params = ("email" -> email) :: ("password" -> password) :: idReq.trackingParameters
    identityApi.post("auth", None, idReq.headers, params, "reauth")(DisregardResponseContent)
  }

  def createUser(userCreationCommand: CreateIdUser)(implicit idReq: IdentityRequest): EitherT[Future, String, UserRegistrationResult] = identityApi.post("user",
    Some(toJson(userCreationCommand)),
    idReq.headers,
    idReq.trackingParameters ++ Seq("authenticate" -> "true", "format" -> "cookies"),
    "create-user") {
    _.json.validate[UserRegistrationResult].asEither.leftMap(_.mkString(","))
  }

  def updateUser(userUpdateCommand: UpdateIdUser, userId: String)(implicit idReq: IdentityRequest): EitherT[Future, String, Unit] = {
    Logger.info(s"Posting updated information to Identity for user :$userId")

    identityApi.post(s"user/$userId",
      Some(toJson(userUpdateCommand)),
      idReq.headers,
      idReq.trackingParameters,
      "update-user")(DisregardResponseContent)
  }
}

trait IdentityApi {

  def getUserPasswordExists(headers:List[(String, String)], parameters: List[(String, String)]) : Future[Boolean] = {
    val endpoint = "user/password-exists"
    val url = s"${Config.idApiUrl}/$endpoint"
    Timing.record(IdentityApiMetrics, "get-user-password-exists") {
      WS.url(url).withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(1000).get().map { response =>
        recordAndLogResponse(response.status, "GET user-password-exists", endpoint)
        (response.json \ "passwordExists").asOpt[Boolean].getOrElse(throw new IdentityApiError(s"$url did not return a boolean"))
      }
    }
  }

  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[Option[IdUser]] = {
    Timing.record(IdentityApiMetrics, "get-user") {
      val url = s"${Config.idApiUrl}/$endpoint"
      WS.url(url).withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(1000).get()
        .recover { case e =>
          Logger.error("Failure trying to retrieve user data", e)
          throw e
        }
        .map { response =>
          recordAndLogResponse(response.status, "GET user", endpoint)
          val jsResult = (response.json \ "user").validate[IdUser]
          if (jsResult.isError) Logger.error(s"Id Api response on $url : ${response.json.toString}")
          jsResult.asOpt
        }
        .recover { case e =>
          Logger.error("Failure trying to deserialise user data", e)
          None
        }
    }
  }

  def post[A](
    endpoint: String, data: Option[JsValue],
    headers: List[(String, String)],
    parameters: List[(String, String)],
    metricName: String)(func: WSResponse => Either[String, A]): EitherT[Future, String, A] = {
      val requestHolder = WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(5000)

      for {
        r <- EitherT.right(Timing.record(IdentityApiMetrics, metricName)(requestHolder.post(data.getOrElse(JsNull))))
        _ = recordAndLogResponse(r.status, s"POST $metricName", endpoint )
        response <- EitherT.fromEither[Future](Either.cond((r.status / 100) == 2, right = r, left = s"Identity API error: POST ${Config.idApiUrl}/$endpoint STATUS ${r.status}"))
        result <- EitherT.fromEither[Future](func(response))
      } yield result
  }

  private def recordAndLogResponse(status: Int, responseMethod: String, endpoint: String) {
    Logger.info(s"$responseMethod response $status for endpoint $endpoint")
    IdentityApiMetrics.putResponseCode(status, responseMethod)
  }
}

object IdentityApi extends IdentityApi

case class IdentityApiError(s: String) extends Throwable {
  override def getMessage: String = s
}
