import actions._
import com.gu.membership.MembershipCatalog
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.services.PromoService
import com.gu.memsub.services.api.{PaymentService, SubscriptionService}
import com.gu.stripe.StripeService
import com.gu.zuora.api.ZuoraService
import com.typesafe.scalalogging.LazyLogging
import play.api.data.Form
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}
import services.api.{MemberService, SalesforceService}

import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

package object controllers extends CommonActions with LazyLogging {

  trait PromoServiceProvider {
    def promoService(implicit request: BackendProvider): PromoService =
      request.touchpointBackend.promoService
  }

  trait MemberServiceProvider {
    def memberService(implicit request: BackendProvider): MemberService =
      request.touchpointBackend.memberService
  }

  trait PaymentServiceProvider {
    def paymentService(implicit request: BackendProvider): PaymentService =
      request.touchpointBackend.paymentService
  }

  trait CatalogProvider {
    def catalog(implicit request: BackendProvider): MembershipCatalog =
      request.touchpointBackend.catalogService.membershipCatalog
  }

  trait StripeServiceProvider {
    def stripeService(implicit request: BackendProvider): StripeService =
      request.touchpointBackend.stripeService
  }

  trait ZuoraServiceProvider {
    def zuoraService(implicit request: BackendProvider): ZuoraService =
      request.touchpointBackend.zuoraService
  }

  trait SalesforceServiceProvider {
    def salesforceService(
        implicit request: BackendProvider): SalesforceService =
      request.touchpointBackend.salesforceService
  }

  trait SubscriptionServiceProvider {
    def subscriptionService(implicit request: BackendProvider
        ): SubscriptionService[MembershipCatalog] =
      request.touchpointBackend.subscriptionService
  }

  implicit class WithRegNumberLabel(m: Member) {
    def regNumberLabel = m.contact.regNumber.getOrElse("")
  }

  def redirectToUnsupportedBrowserInfo[T : ClassTag](form: Form[T])(
      implicit req: RequestHeader): Future[Result] = {
    lazy val errors = form.errors.map { e =>
      s"  - ${e.key}: ${e.messages.mkString(", ")}"
    }.mkString("\n")
    logger.error(
        s"Server-side form errors on joining indicates a Javascript problem: ${req.headers
      .get(USER_AGENT)}")
    logger.error(
        s"Server-side form errors : Failed to bind from form ${classTag[T]}:\n$errors")
    Future(Redirect(routes.Joiner.unsupportedBrowser()))
  }
}
