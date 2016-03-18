package services

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.lib.okhttpscala._
import com.gu.memsub.Address
import com.gu.memsub.util.Timing
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp._
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import monitoring.IdentityApiMetrics
import play.api.Logger
import play.api.libs.json._
import views.support.IdentityUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class IdentityServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

case class IdentityService(identityApi: IdentityApi) {
  def getIdentityUserView(user: IdMinimalUser, identityRequest: IdentityRequest): Future[IdentityUser] =
    getFullUserDetails(user, identityRequest)
      .zip(doesUserPasswordExist(identityRequest))
      .map { case (fullUser, doesPasswordExist) =>
        IdentityUser(fullUser, doesPasswordExist)
      }

  def getFullUserDetails(user: IdMinimalUser, identityRequest: IdentityRequest): Future[IdUser] =
    identityApi.get(s"user/${user.id}", identityRequest.headers, identityRequest.trackingParameters)
      .map(_.getOrElse(throw IdentityServiceError(s"Couldn't find user with ID ${user.id}")))

  def doesUserPasswordExist(identityRequest: IdentityRequest): Future[Boolean] =
    identityApi.getUserPasswordExists(identityRequest.headers, identityRequest.trackingParameters)

  def updateUserFieldsBasedOnJoining(user: IdMinimalUser, formData: JoinForm, identityRequest: IdentityRequest) {

    val billingDetails = formData match {
      case billingForm: PaidMemberJoinForm =>
        val billingAddressForm = billingForm.billingAddress.getOrElse(billingForm.deliveryAddress)
        billingAddress(billingAddressForm)
      case _ => Json.obj()
    }

    val fields = Json.obj(
      "secondName" -> formData.name.last,
      "firstName" -> formData.name.first
    ) ++ deliveryAddress(formData.deliveryAddress) ++ billingDetails

    val json = Json.obj("privateFields" -> fields)
    postFields(json, user.id, identityRequest)
  }

  def updateUserPassword(password: String, identityRequest: IdentityRequest, userId: String) {
    val json = Json.obj("newPassword" -> password)
    identityApi.post("/user/password", Some(json), identityRequest.headers, identityRequest.trackingParameters, "update-user-password")
  }

  def updateUserFieldsBasedOnUpgrade(userId: String, addressDetails: AddressDetails, identityRequest: IdentityRequest) {
    val billingAddressForm = addressDetails.billingAddress.getOrElse(addressDetails.deliveryAddress)
    val fields = deliveryAddress(addressDetails.deliveryAddress) ++ billingAddress(billingAddressForm)
    val json = Json.obj("privateFields" -> fields)
    postFields(json, userId, identityRequest)
  }

  def updateEmail(user: IdMinimalUser, email: String, identityRequest: IdentityRequest) = {
    val json = Json.obj("primaryEmailAddress" -> email)
    postFields(json, user.id, identityRequest)
  }

  def reauthUser(email: String, password: String, identityRequest: IdentityRequest) = {
    val params = ("email" -> email) :: ("password" -> password) :: identityRequest.trackingParameters
    identityApi.post("auth", None, identityRequest.headers, params, "reauth")

  }

  private def postFields(json: JsObject, userId: String, identityRequest: IdentityRequest) = {
    Logger.info(s"Posting updated information to Identity for user :$userId")
    identityApi.post(s"user/$userId", Some(json), identityRequest.headers, identityRequest.trackingParameters, "update-user")
  }

  private def deliveryAddress(addressForm: Address): JsObject = {
    Json.obj(
      "address1" -> addressForm.lineOne,
      "address2" -> addressForm.lineTwo,
      "address3" -> addressForm.town,
      "address4" -> addressForm.countyOrState,
      "postcode" -> addressForm.postCode,
      "country" -> addressForm.country.name
    )
  }

  private def billingAddress(billingAddress: Address): JsObject = {
    Json.obj(
      "billingAddress1" -> billingAddress.lineOne,
      "billingAddress2" -> billingAddress.lineTwo,
      "billingAddress3" -> billingAddress.town,
      "billingAddress4" -> billingAddress.countyOrState,
      "billingPostcode" -> billingAddress.postCode,
      "billingCountry" -> billingAddress.country.name
    )
  }
}

trait IdentityApi {

  val okhttp = new OkHttpClient()

  val JsonMediaType = MediaType.parse("application/json; charset=utf-8")

  implicit def jsonToRequestBody(json: JsValue): RequestBody = RequestBody.create(JsonMediaType, json.toString)


  def requestFor(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]): Request.Builder = {
    val url = parameters.foldLeft(HttpUrl.parse(s"${Config.idApiUrl}/$endpoint").newBuilder()) {
      case (u, (k, v)) => u.addQueryParameter(k, v)
    }.build()

    headers.foldLeft(new Builder().url(url)) {
      case (r, (k,v)) => r.addHeader(k, v)
    }
  }

  def getUserPasswordExists(headers:List[(String, String)], parameters: List[(String, String)]) : Future[Boolean] = {
    Timing.record(IdentityApiMetrics, "get-user-password-exists") {
      val endpoint = "user/password-exists"
      okhttp.execute(requestFor(endpoint, headers, parameters).build()).map { response =>
        recordAndLogResponse(response.code, "GET user-password-exists", endpoint)
        (Json.parse(response.body.string) \ "passwordExists").asOpt[Boolean].getOrElse(throw new IdentityApiError(s"${response.request.urlString} did not return a boolean"))
      }
    }
  }

  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[Option[IdUser]] = {
    Timing.record(IdentityApiMetrics, "get-user") {
      okhttp.execute(requestFor(endpoint, headers, parameters).build())
        .recover { case e =>
          Logger.error("Failure trying to retrieve user data", e)
          throw e
        }
        .map { response =>
          recordAndLogResponse(response.code, "GET user", endpoint)
          val jsResult = (Json.parse(response.body.string) \ "user").validate[IdUser]
          if (jsResult.isError) Logger.error(s"Id Api response on ${response.request.urlString} : $jsResult")
          jsResult.asOpt
        }
        .recover { case e =>
          Logger.error("Failure trying to deserialise user data", e)
          None
        }
    }
  }

  def post(endpoint: String, data: Option[JsObject], headers: List[(String, String)], parameters: List[(String, String)], metricName: String): Future[Int] = {
    Timing.record(IdentityApiMetrics, metricName) {
      for {
        response <- okhttp.execute(requestFor(endpoint, headers, parameters).post(data.getOrElse[JsValue](JsNull)).build())
      } yield {
        recordAndLogResponse(response.code, s"POST $metricName", endpoint )
        response.code
      }
    }
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
