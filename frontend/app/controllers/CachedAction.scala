package controllers

import play.api.mvc.{ SimpleResult, Request, ActionBuilder }
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CachedAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = block(request).map { Cached(_) }
}

object NoCacheAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = block(request).map { NoCache(_) }
}
