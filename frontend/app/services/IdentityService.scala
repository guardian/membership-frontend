package services

import com.gu.identity.model.User
import configuration.Config
import forms.MemberForm.JoinForm
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

  def updateUser(user: User, formData: JoinForm, cookieOpt: Option[Cookie]): Future[WSResponse] = {
    cookieOpt.map { cookie =>
      IdentityApi.post(s"user/${user.id}", updateUser(user, formData), cookie.value)
    }.getOrElse(throw IdentityServiceError("User cookie not set"))
  }

  private def updateUser(user: User, formData: JoinForm) = {
    Json.obj("privateFields" ->
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