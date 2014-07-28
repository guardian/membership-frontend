package model

import scala.xml.Elem
import org.joda.time.DateTime

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.Tier

object Zuora {
  trait ZuoraObject

  case class Authentication(token: String, url: String) extends ZuoraObject

  object Authentication {
    def login(user: String, pass: String): Elem = {
      <api:login>
        <api:username>{user}</api:username>
        <api:password>{pass}</api:password>
      </api:login>
    }

    def apply(elem: Elem): Authentication = {
      val result = elem \\ "loginResponse" \ "result"
      Authentication((result \ "Session").text, (result \ "ServerUrl").text)
    }
  }

}
