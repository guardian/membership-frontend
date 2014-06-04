package controllers

import play.api.mvc.SimpleResult
import play.api.http.HeaderNames
import configuration.Config

object Cors {
  def apply(result: SimpleResult) = result.withHeaders(
    HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "GET",
    HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
  )
}

