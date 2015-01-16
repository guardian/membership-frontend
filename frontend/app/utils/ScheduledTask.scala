package utils

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent

import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.Logger

trait ScheduledTask[T] {
  val initialValue: T

  val initialDelay: FiniteDuration
  val interval: FiniteDuration

  val name = getClass.getSimpleName

  private implicit val system = Akka.system
  lazy val agent = Agent[T](initialValue)

  def refresh(): Future[T]

  def start() {
    Logger.debug(s"Starting $name scheduled task")
    system.scheduler.schedule(initialDelay, interval) {
      agent.sendOff { _ =>
        Logger.debug(s"Refreshing $name scheduled task")
        Await.result(refresh(), 15.seconds)
      }
    }
  }

  def get() = agent.get()
}

object ScheduledTask {
  def apply[T](taskName: String, initValue: T, initDelay: FiniteDuration, intervalPeriod: FiniteDuration)(refresher: => Future[T]) =
    new ScheduledTask[T] {
      val initialValue = initValue
      val initialDelay = initDelay
      val interval = intervalPeriod

      override val name = taskName

      def refresh(): Future[T] = refresher
    }
}
