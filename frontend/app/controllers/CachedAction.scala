package controllers

import play.api.mvc.{ Result, Request, ActionBuilder }
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CachedAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map { Cached(_) }
}

object NoCacheAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = block(request).map { NoCache(_) }
}
