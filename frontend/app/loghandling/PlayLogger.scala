package loghandling

import play.api.{Logger, MarkerContext}

object PlayLogger {
  val logger = Logger("application")

  def info(message: String)(implicit mc: MarkerContext): Unit = logger.info(message)
}
