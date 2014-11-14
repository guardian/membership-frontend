package services

import com.netaporter.uri.Uri
import scala.concurrent.Future

case class StripeCredentials(secretKey: String, publicKey: String)
import play.api.libs.ws.WSRequestHolder

case class StripeApiConfig(url: Uri, credentials: StripeCredentials)
import model.Stripe._
import model.StripeDeserializer._
import monitoring.StripeMetrics


class StripeService(val apiConfig: StripeApiConfig) extends utils.WebServiceHelper[StripeObject, Error] {
  val wsUrl = apiConfig.url.toString()
  val wsMetrics = StripeMetrics
  val publicKey = apiConfig.credentials.publicKey

  def wsPreExecute(req: WSRequestHolder): WSRequestHolder =
    req.withHeaders("Authorization" -> s"Bearer ${apiConfig.credentials.secretKey}")

  object Customer {
    def create(identityId: String, card: String): Future[Customer] =
      post[Customer]("customers", Map("description" -> Seq(s"IdentityID - $identityId"), "card" -> Seq(card)))

    def read(customerId: String): Future[Customer] =
      get[Customer](s"customers/$customerId")

    def updateCard(customerId: String, card: String): Future[Customer] =
      post[Customer](s"customers/$customerId", Map("card" -> Seq(card)))
  }
}
