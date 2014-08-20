package services

import com.gu.identity.model.User
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.IdentityUser
import model.UserDeserializer._
import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IdentityService {

  def getFullUserDetails(user: User, identityRequest: IdentityRequest): Future[IdentityUser] = {
    IdentityApi.get(s"user/${user.id}", identityRequest.headers, identityRequest.trackingParameters)
  }

  def updateUserBasedOnJoining(user: User, formData: JoinForm, identityRequest: IdentityRequest): Future[WSResponse] = {

    val billingDetails = if (formData.isInstanceOf[PaidMemberJoinForm]) {
      val billingForm = formData.asInstanceOf[PaidMemberJoinForm]
      val billingAddressForm = billingForm.billingAddress.getOrElse(billingForm.deliveryAddress)
      billingAddress(billingAddressForm)
    } else Json.obj()

    val fields = Json.obj(
      "secondName" -> formData.name.last,
      "firstName" -> formData.name.first
    ) ++ deliveryAddress(formData.deliveryAddress) ++ billingDetails

    postRequest(fields, user, identityRequest)
  }

  def updateUserBasedOnUpgrade(user: User, formData: PaidMemberChangeForm, identityRequest: IdentityRequest) = {

    val billingAddressForm = formData.billingAddress.getOrElse(formData.deliveryAddress)
    val fields = deliveryAddress(formData.deliveryAddress) ++ billingAddress(billingAddressForm)
    postRequest(fields, user, identityRequest)
  }

  private def postRequest(fields: JsObject, user: User, identityRequest: IdentityRequest) = {
    val json = Json.obj("privateFields" -> fields)

    Logger.info(s"Posting updated information to Identity for user :${user.id}")
    IdentityApi.post(s"user/${user.id}", json, identityRequest.headers, identityRequest.trackingParameters)
  }

  private def deliveryAddress(addressForm: AddressForm): JsObject = {
    Json.obj(
      "address1" -> addressForm.lineOne,
      "address2" -> addressForm.lineTwo,
      "address3" -> addressForm.town,
      "address4" -> addressForm.countyOrState,
      "postcode" -> addressForm.postCode,
      "country" -> addressForm.country
    )
  }

  private def billingAddress(billingAddress: AddressForm): JsObject = {
    Json.obj(
      "billingAddress1" -> billingAddress.lineOne,
      "billingAddress2" -> billingAddress.lineTwo,
      "billingAddress3" -> billingAddress.town,
      "billingAddress4" -> billingAddress.countyOrState,
      "billingPostcode" -> billingAddress.postCode,
      "billingCountry" -> billingAddress.postCode
    )
  }
}

object IdentityService extends IdentityService

trait Http {
  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[IdentityUser]

  def post(endpoint: String, data: JsObject, headers: List[(String, String)], parameters: List[(String, String)]): Future[WSResponse]

}

object IdentityApi extends Http {

  def get(endpoint: String, headers:List[(String, String)], parameters: List[(String, String)]) : Future[IdentityUser] = {
    WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).get().map { response =>
       (response.json \ "user").as[IdentityUser] //todo handle error
    }
  }

  def post(endpoint: String, data: JsObject, headers: List[(String, String)], parameters: List[(String, String)]): Future[WSResponse] = {

    WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withQueryString(parameters: _*).withRequestTimeout(2000).post(data)
  }
}