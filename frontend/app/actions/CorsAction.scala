package actions

import scala.concurrent.Future

import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.http.HeaderNames
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import configuration.Config

object CorsAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    block(request).map(_.withHeaders(Cors.headers: _*))
  }
}

object Cors {
  val headers = Seq(
    HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "GET",
    HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true",
    HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS -> "Csrf-Token"
  )
}
