package services

import cats.data.EitherT
import cats.instances.all._
import cats.syntax.either._
import com.gu.identity.play._
import com.gu.identity.play.idapi.{CreateIdUser, UpdateIdUser, UserRegistrationResult}
import com.gu.memsub.Address
import com.gu.memsub.util.Timing
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.IdentityRequest
import dispatch.Defaults.timer
import dispatch._
import forms.MemberForm._
import monitoring.IdentityApiMetrics
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import services.IdentityService.DisregardResponseContent
import views.support.IdentityUser

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class IdentityServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object IdentityService extends LazyLogging {
  val DisregardResponseContent: (WSResponse => Either[String, Unit]) = {
    resp =>
      logger.debug(s"Webservice returned code ${resp.status}, and body: ${resp.body}")
      Right(Unit)
  }

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

  def apply(identityApiProvider: => IdentityApi)(implicit ec: ExecutionContext) =
    new IdentityService(identityApiProvider, ec)
}

class IdentityService(identityApiProvider: => IdentityApi, implicit val ec: ExecutionContext) {

  lazy val identityApi = identityApiProvider

  def getIdentityUserView(user: IdMinimalUser, identityRequest: IdentityRequest): Future[IdentityUser] =
    getFullUserDetails(user)(identityRequest)
      .zip(doesUserPasswordExist(identityRequest))
      .map { case (fullUser, doesPasswordExist) =>
        IdentityUser(fullUser, doesPasswordExist)
      }

  def doesUserExist(email: String)(implicit idReq: IdentityRequest): Future[Boolean] = (identityApi.get("user",
    idReq.headers,
    idReq.trackingParameters :+ ("emailAddress" -> email),
    "get-user-by-email") { resp =>
    Right(resp.status == Status.OK)
  }).valueOr(_ => false)

  def getFullUserDetails(user: IdMinimalUser)(implicit identityRequest: IdentityRequest): Future[IdUser] =
    retry.Backoff(max = 3, delay = 2.seconds, base = 2){ () =>
      getUser(user).value
    }.map(_.fold({ errorMessage =>
      Logger.warn(s"identity get user failed with: $errorMessage")
      val guCookieExists = identityRequest.headers.exists(_._1 == "X-GU-ID-FOWARDED-SC-GU-U")
      val guTokenExists = identityRequest.headers.exists(_._1 == "Authorization")
      val errContext = s"SC_GU_U=$guCookieExists GU-IdentityToken=$guTokenExists trackingParamters=${identityRequest.trackingParameters.toString}"
      throw IdentityServiceError(s"Couldn't get user's ${user.id} full details. $errContext")
    }, identity))

  def getUser(user: IdMinimalUser)(implicit idReq: IdentityRequest): EitherT[Future, String, IdUser] = identityApi.get(s"user/${user.id}",
    idReq.headers,
    idReq.trackingParameters,
    "get-user") { resp =>
      (resp.json \ "user").validate[IdUser].asEither.leftMap(_.mkString(","))
    }

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

  def consentEmail(email: String, idReq: IdentityRequest): EitherT[Future, String, Unit] = {
    identityApi.post("/consent-email",
      Some(Json.obj("email" -> email, "set-consents" -> List("supporter"))),
      idReq.headers,
      Nil, "consentEmail")(DisregardResponseContent)
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

class IdentityApi(val wsClient: WSClient, implicit val ec: ExecutionContext) {

  def getUserPasswordExists(headers:List[(String, String)], parameters: List[(String, String)]) : Future[Boolean] = {
    val endpoint = "user/password-exists"
    val url = s"${Config.idApiUrl}/$endpoint"
    Timing.record(IdentityApiMetrics, "get-user-password-exists") {
      wsClient.url(url).withHttpHeaders(headers: _*).withQueryStringParameters(parameters: _*).withRequestTimeout(1000 milli).get().map { response =>
        recordAndLogResponse(response.status, "GET user-password-exists", endpoint)
        (response.json \ "passwordExists").asOpt[Boolean].getOrElse(throw new IdentityApiError(s"$url did not return a boolean"))
      }
    }
  }

  def get[A](
    endpoint: String,
    headers: List[(String, String)],
    parameters: List[(String, String)],
    metricName: String)(func: WSResponse => Either[String, A]): EitherT[Future, String, A] = {
    execute(endpoint, metricName, func,
      wsClient.url(s"${Config.idApiUrl}/$endpoint").withHttpHeaders(headers: _*).withQueryStringParameters(parameters: _*).withRequestTimeout(1000 milli).withMethod("GET"))
  }

  def post[A](
    endpoint: String, data: Option[JsValue],
    headers: List[(String, String)],
    parameters: List[(String, String)],
    metricName: String)(func: WSResponse => Either[String, A]): EitherT[Future, String, A] = {
    execute(endpoint, metricName, func,
      wsClient.url(s"${Config.idApiUrl}/$endpoint").withHttpHeaders(headers: _*).withQueryStringParameters(parameters: _*)
        .withRequestTimeout(5000 milli).withMethod("POST").withBody(data.getOrElse(JsNull)))
  }

  private def execute[A](endpoint: String, metricName: String, func: (WSResponse) => Either[String, A], requestHolder: WSRequest): EitherT[Future, String, A] = for {
    r <- EitherT.right(Timing.record(IdentityApiMetrics, metricName)(requestHolder.execute()))
    _ = recordAndLogResponse(r.status, s"${requestHolder.method} $metricName", endpoint)
    response <- EitherT.fromEither[Future](Either.cond((r.status / 100) == 2, right = r, left = s"Identity API error: ${requestHolder.method} ${Config.idApiUrl}/$endpoint STATUS ${r.status}"))
    result <- EitherT.fromEither[Future](func(response))
  } yield result

  private def recordAndLogResponse(status: Int, responseMethod: String, endpoint: String) {
    Logger.info(s"$responseMethod response $status for endpoint $endpoint")
    IdentityApiMetrics.putResponseCode(status, responseMethod)
  }
}

case class IdentityApiError(s: String) extends Throwable {
  override def getMessage: String = s
}
