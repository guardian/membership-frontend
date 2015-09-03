package utils

import java.util.concurrent.TimeoutException

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

  def start(timeout : FiniteDuration = 25.seconds) {
    Logger.info(s"Starting $name scheduled task with an initial delay of ${initialDelay} interval ${interval} and a timeout of ${timeout}")
    system.scheduler.schedule(initialDelay, interval) {
      agent.sendOff { _ =>
        Logger.info(s"Refreshing $name scheduled task")
        try {
          Await.result(refresh(), timeout)
        }
        catch {
          case ex : TimeoutException => {
            Logger.error(s"Scheduled task ${name} timed out after 25 seconds")
            throw ex
          }
        }
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
