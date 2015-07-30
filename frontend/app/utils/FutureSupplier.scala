package utils

import akka.agent.Agent

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class FutureSupplier[T](f: => Future[T]) {
  private val agent = Agent(f)

  def refresh(): Future[T] = for {
    refreshAlteringFuture <- agent.alter { currentRefresh =>
      if (currentRefresh.isCompleted) f else currentRefresh
    }
    refreshFuture <- refreshAlteringFuture
  } yield refreshFuture

  def get(): Future[T] = agent.get()
}
