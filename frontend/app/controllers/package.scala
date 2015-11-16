import actions._
import com.gu.membership.salesforce.{PaidTierMember, Member}
import com.typesafe.scalalogging.LazyLogging
import play.api.data.Form
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.Redirect
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.reflect.classTag

package object controllers extends CommonActions with LazyLogging{

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
