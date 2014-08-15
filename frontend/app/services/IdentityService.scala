package services

import com.gu.identity.model.User
import configuration.Config
import forms.MemberForm._
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.{WS, WSResponse}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Cookie


import scala.concurrent.Future

case class IdentityServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait IdentityService {

  def updateUserBasedOnJoining(user: User, formData: JoinForm, cookieOpt: Option[Cookie]): Future[WSResponse] = {
    cookieOpt.map { cookie =>

      val basicFields = Json.obj(
        "secondName" -> formData.name.last,
        "firstName" -> formData.name.first       
      ) ++ deliveryAddress(formData.deliveryAddress)

      val fields = if(formData.isInstanceOf[PaidMemberJoinForm]) {
        val billingForm = formData.asInstanceOf[PaidMemberJoinForm]
        val billingAddressForm = billingForm.billingAddress.getOrElse(billingForm.deliveryAddress)
        basicFields ++ billingAddress(billingAddressForm)
      } else basicFields

       postRequest(fields, user, cookie)

    }.getOrElse(throw IdentityServiceError("User cookie not set"))
  }

  def updateUserBasedOnUpgrade(user:User, formData: PaidMemberChangeForm, cookieOpt:Option[Cookie]) = {
    cookieOpt.map { cookie =>

      val billingAddressForm = formData.billingAddress.getOrElse(formData.deliveryAddress)

      val fields = deliveryAddress(formData.deliveryAddress) ++ billingAddress(billingAddressForm)

      postRequest(fields, user, cookie)

    }.getOrElse(throw IdentityServiceError("User cookie not set"))
  }

  private def postRequest(fields: JsObject, user: User, cookie: Cookie) = {
    val json = Json.obj("privateFields" -> fields)

    Logger.info(s"Posting updated information to Identity for user :${user.id}")
    IdentityApi.post(s"user/${user.id}", json, cookie.value)
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
  def post(endpoint: String, data: JsObject, cookieValue: String): Future[WSResponse]
}

object IdentityApi extends Http {

  def post(endpoint: String, data: JsObject, identityCookieValue: String): Future[WSResponse] = {

    val headers = List(("X-GU-ID-Client-Access-Token" -> s"Bearer ${Config.idApiClientToken}"), ("X-GU-ID-FOWARDED-SC-GU-U" -> identityCookieValue))

    WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withRequestTimeout(2000).post(data)
  }
}