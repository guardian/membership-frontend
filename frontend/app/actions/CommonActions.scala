package actions

import actions.Functions._
import controllers.{Cached, NoCache}

trait CommonActions {
  val NoCacheAction = resultModifier(NoCache(_))

  val CachedAction = resultModifier(Cached(_))
}
