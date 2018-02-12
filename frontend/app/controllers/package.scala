import actions._
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.services.api.PaymentService
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.subsv2.services._
import com.gu.stripe.StripeService
import com.gu.zuora.ZuoraRestService
import com.gu.zuora.api.ZuoraService
import com.typesafe.scalalogging.LazyLogging
import play.api.data.Form
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, RequestHeader, Result}
import services.{PayPalService, TouchpointBackends}
import services.api.{MemberService, SalesforceService}

import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

package object controllers extends CommonActions with LazyLogging{

  trait MemberServiceProvider {
    def memberService(implicit request: BackendProvider, tpbs: TouchpointBackends): MemberService =
      request.touchpointBackend.memberService
  }

  trait PaymentServiceProvider {
    def paymentService(implicit request: BackendProvider, tpbs: TouchpointBackends): PaymentService =
      request.touchpointBackend.paymentService
  }

  trait CatalogProvider {
    def catalog(implicit request: BackendProvider, tpbs: TouchpointBackends): Catalog =
      request.touchpointBackend.catalogService.unsafeCatalog
  }

  trait StripeUKMembershipServiceProvider {
    def stripeUKMembershipService(implicit request: BackendProvider, tpbs: TouchpointBackends): StripeService =
      request.touchpointBackend.stripeUKMembershipService
  }

  trait StripeAUMembershipServiceProvider {
    def stripeAUMembershipService(implicit request: BackendProvider, tpbs: TouchpointBackends): StripeService =
      request.touchpointBackend.stripeAUMembershipService
  }

  trait PayPalServiceProvider {
    def payPalService(implicit request: AuthRequest[AnyContent], tpbs: TouchpointBackends): PayPalService =
      request.touchpointBackend.payPalService
  }

  trait ZuoraServiceProvider {
    def zuoraService(implicit request: BackendProvider, tpbs: TouchpointBackends): ZuoraService =
      request.touchpointBackend.zuoraService
  }

  trait ZuoraRestServiceProvider {
    def zuoraRestService(implicit request: BackendProvider, tpbs: TouchpointBackends): ZuoraRestService[Future] =
      request.touchpointBackend.zuoraRestService
  }

  trait SalesforceServiceProvider {
    def salesforceService(implicit request: BackendProvider, tpbs: TouchpointBackends): SalesforceService =
      request.touchpointBackend.salesforceService
  }

  trait SubscriptionServiceProvider {
    def subscriptionService(implicit request: BackendProvider, tpbs: TouchpointBackends): SubscriptionService[Future] =
      request.touchpointBackend.subscriptionService
  }

  implicit class WithRegNumberLabel(m: Member) {
    def regNumberLabel = m.contact.regNumber.getOrElse("")
  }

  def redirectToUnsupportedBrowserInfo[T: ClassTag](form: Form[T])(implicit req: RequestHeader): Future[Result] = {
    lazy val errors = form.errors.map { e => s"  - ${e.key}: ${e.messages.mkString(", ")}"}.mkString("\n")
    logger.error(s"Server-side form errors on joining indicates a Javascript problem: ${req.headers.get(USER_AGENT)}")
    logger.error(s"Server-side form errors : Failed to bind from form ${classTag[T]}:\n$errors")
    Future(Redirect(routes.Joiner.unsupportedBrowser()))
  }
}
