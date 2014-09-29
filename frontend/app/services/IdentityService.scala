package services

import com.gu.identity.model.User
import com.gu.membership.util.Timing
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.IdentityUser
import model.UserDeserializer._
import monitoring.IdentityApiMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.gu.membership.zuora.Countries

trait IdentityService {

  def getFullUserDetails(user: User, identityRequest: IdentityRequest): Future[Option[IdentityUser]] =
    IdentityApi.get(s"user/${user.id}", identityRequest.headers, identityRequest.trackingParameters)

  def doesUserPasswordExist(identityRequest: IdentityRequest): Future[Boolean] =
    IdentityApi.getUserPasswordExists(identityRequest.headers, identityRequest.trackingParameters)

  def updateUserFieldsBasedOnJoining(user: User, formData: JoinForm, identityRequest: IdentityRequest) {

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

    postFields(fields, user, identityRequest)
  }

  def updateUserPassword(password: String, identityRequest: IdentityRequest, userId: String) {
    Timing.record(IdentityApiMetrics, "update-user-password") {
      val json = Json.obj("newPassword" -> password)
      IdentityApi.post("/user/password", json, identityRequest.headers, identityRequest.trackingParameters, "update-user-password")
    }
  }

  def updateUserFieldsBasedOnUpgrade(user: User, formData: PaidMemberChangeForm, identityRequest: IdentityRequest) = {
    val billingAddressForm = formData.billingAddress.getOrElse(formData.deliveryAddress)
    val fields = deliveryAddress(formData.deliveryAddress) ++ billingAddress(billingAddressForm)
    postFields(fields, user, identityRequest)
  }

  private def postFields(fields: JsObject, user: User, identityRequest: IdentityRequest) = {
    val json = Json.obj("privateFields" -> fields)
    Logger.info(s"Posting updated information to Identity for user :${user.id}")
    IdentityApi.post(s"user/${user.id}", json, identityRequest.headers, identityRequest.trackingParameters, "update-user")
  }

  private def deliveryAddress(addressForm: AddressForm): JsObject = {
    Json.obj(
      "address1" -> addressForm.lineOne,
      "address2" -> addressForm.lineTwo,
      "address3" -> addressForm.town,
      "address4" -> addressForm.countyOrState,
      "postcode" -> addressForm.postCode,
      "country" -> addressForm.country.name
    )
  }

  private def billingAddress(billingAddress: AddressForm): JsObject = {
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

object IdentityService extends IdentityService

trait Http {
  def getUserPasswordExists(headers:List[(String, String)], parameters: List[(String, String)]) : Future[Boolean]

  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[Option[IdentityUser]]

  def post(endpoint: String, data: JsObject, headers: List[(String, String)], parameters: List[(String, String)], metricName: String): Future[WSResponse]

}

object IdentityApi extends Http {

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

  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[Option[IdentityUser]] = {
    Timing.record(IdentityApiMetrics, "get-user") {
      WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(1000).get().map { response =>
        recordAndLogResponse(response.status, "GET user", endpoint)
        (response.json \ "user").asOpt[IdentityUser]
      }.recover {
        case _ => None
      }
    }
  }

  def post(endpoint: String, data: JsObject, headers: List[(String, String)], parameters: List[(String, String)], metricName: String): Future[WSResponse] = {
    Timing.record(IdentityApiMetrics, "post-user") {
      val response = WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(2000).post(data)
      response.map (r => recordAndLogResponse(r.status, s"POST $metricName", endpoint ))
      response
    }
  }

  private def recordAndLogResponse(status: Int, responseMethod: String, endpoint: String) {
    Logger.info(s"$responseMethod response ${status} for endpoint ${endpoint}")
    IdentityApiMetrics.putResponseCode(status, responseMethod)
  }
}

case class IdentityApiError(s: String) extends Throwable {
  override def getMessage: String = s
}