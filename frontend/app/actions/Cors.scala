package actions

import scala.language.higherKinds
import scala.concurrent.Future

import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import configuration.Config

object Cors {
  val headers = Seq(
    HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "GET",
    HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
  )

  def apply[R[A]](ab: ActionBuilder[R]): ActionBuilder[R] = {
    new ActionBuilder[R] {
      def invokeBlock[A](request: Request[A], block: R[A] => Future[Result]): Future[Result] =
        ab.invokeBlock(request, block).map(_.withHeaders(headers: _*))
    }
  }
}