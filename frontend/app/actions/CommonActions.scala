package actions

import actions.Functions._
import configuration.Config
import controllers.{Cached, NoCache}
import play.api.http.HeaderNames._

trait CommonActions {
  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))

  val Cors = resultModifier(_.withHeaders(
    ACCESS_CONTROL_ALLOW_ORIGIN -> Config.corsAllowOrigin,
    ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"))
}
