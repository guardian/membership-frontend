package services

import actions.AuthRequest
import com.gu.identity.model.User
import configuration.Config
import forms.MemberForm._
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.{WS, WSResponse}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Request, Cookie}


import scala.concurrent.Future

case class IdentityServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait IdentityService {

  def updateUserBasedOnJoining(user: User, formData: JoinForm, identityHeaders: List[(String, String)]): Future[WSResponse] = {

      val billingDetails = if(formData.isInstanceOf[PaidMemberJoinForm]) {
        val billingForm = formData.asInstanceOf[PaidMemberJoinForm]
        val billingAddressForm = billingForm.billingAddress.getOrElse(billingForm.deliveryAddress)
        billingAddress(billingAddressForm)
      } else Json.obj()

      val fields = Json.obj(
        "secondName" -> formData.name.last,
        "firstName" -> formData.name.first
      ) ++ deliveryAddress(formData.deliveryAddress) ++ billingDetails

       postRequest(fields, user, identityHeaders)
  }

  def updateUserBasedOnUpgrade(user:User, formData: PaidMemberChangeForm, identityHeaders: List[(String, String)]) = {

      val billingAddressForm = formData.billingAddress.getOrElse(formData.deliveryAddress)
      val fields = deliveryAddress(formData.deliveryAddress) ++ billingAddress(billingAddressForm)
      postRequest(fields, user, identityHeaders)
  }

  private def postRequest(fields: JsObject, user: User, identityHeaders: List[(String, String)]) = {
    val json = Json.obj("privateFields" -> fields)

    Logger.info(s"Posting updated information to Identity for user :${user.id}")
    IdentityApi.post(s"user/${user.id}", json, identityHeaders)
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
  def post(endpoint: String, data: JsObject, headers: List[(String, String)]): Future[WSResponse]
}

object IdentityApi extends Http {

  def post(endpoint: String, data: JsObject, headers: List[(String, String)] = List.empty): Future[WSResponse] = {

    WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withRequestTimeout(2000).post(data)
  }
}