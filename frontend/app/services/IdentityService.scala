package services

import com.gu.identity.model.User
import configuration.Config
import forms.MemberForm.JoinForm
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.{WS, WSResponse}
import play.api.libs.json.{JsObject, Json}


import scala.concurrent.Future

trait IdentityService {

  def saveUser(user: User, formData: JoinForm, cookieValue: String): Future[WSResponse] = {
    IdentityApi.post(s"user/${user.id}/privateFields", updateUser(user, formData), cookieValue)
  }

  private def updateUser(user: User, formData: JoinForm) = {
    Json.obj(
      "secondName" -> formData.name.last,
      "firstName" -> formData.name.first,
      "address1" -> formData.deliveryAddress.lineOne,
      "address2" -> formData.deliveryAddress.lineTwo,
      "address3" -> formData.deliveryAddress.town,
      "address4" -> formData.deliveryAddress.countyOrState,
      "postcode" -> formData.deliveryAddress.postCode,
      "country" -> formData.deliveryAddress.country
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

    WS.url(s"${Config.idApiUrl}/$endpoint").withHeaders(headers: _*).withRequestTimeout(1000).post(data)
  }
}