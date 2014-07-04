package controllers

import play.api.mvc.Result
import play.api.http.HeaderNames
import configuration.Config

object Cors {
  def apply(result: Result) = result.withHeaders(
    HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "GET",
    HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
  )
}
