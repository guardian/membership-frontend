package services

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.memsub.util.Timing
import com.gu.memsub.Address
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import monitoring.IdentityApiMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.WS
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

  def post(endpoint: String, data: Option[JsObject], headers: List[(String, String)], parameters: List[(String, String)], metricName: String): Future[Int] = {
    Timing.record(IdentityApiMetrics, metricName) {
      val requestHolder = WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(5000)
      val response = requestHolder.post(data.getOrElse(JsNull))
      response.foreach(r => recordAndLogResponse(r.status, s"POST $metricName", endpoint ))
      response.map(_.status)
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
