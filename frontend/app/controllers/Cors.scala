package controllers

import play.api.mvc.{Controller, Result}
import play.api.http.HeaderNames
import configuration.Config
import actions.CorsAction

object Cors extends Controller {
  def apply(result: Result) = result.withHeaders(
    HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "GET",
    HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
  )

  def options = CorsAction { Cached(Ok) }
}
