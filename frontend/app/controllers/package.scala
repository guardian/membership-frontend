import actions._
import com.gu.membership.salesforce.{Member, PaidTierMember}
import com.gu.membership.stripe.StripeService
import com.typesafe.scalalogging.LazyLogging
import model.MembershipCatalog
import play.api.data.Form
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}
import services.SubscriptionService

import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

package object controllers extends CommonActions with LazyLogging{

  trait SubscriptionServiceProvider {
    def subscriptionService(implicit request: BackendProvider): SubscriptionService =
      request.touchpointBackend.subscriptionService
  }

  trait CatalogProvider {
    def catalog(implicit request: BackendProvider): Future[MembershipCatalog] =
      request.touchpointBackend.subscriptionService.membershipCatalog.get
  }

  trait StripeServiceProvider {
    def stripeService(implicit request: BackendProvider): StripeService =
      request.touchpointBackend.stripeService
  }

  implicit class WithRegNumberLabel(m: Member) {
    def regNumberLabel = m match {
      case PaidTierMember(n, _) => n
      case _ => ""
    }
  }

  def redirectToUnsupportedBrowserInfo[T: ClassTag](form: Form[T])(implicit req: RequestHeader): Future[Result] = {
    lazy val errors = form.errors.map { e => s"  - ${e.key}: ${e.messages.mkString(", ")}"}.mkString("\n")
    logger.error(s"Server-side form errors on joining indicates a Javascript problem: ${req.headers.get(USER_AGENT)}")
    logger.error(s"Server-side form errors : Failed to bind from form ${classTag[T]}:\n$errors")
    Future(Redirect(routes.Joiner.unsupportedBrowser()))
  }
}
