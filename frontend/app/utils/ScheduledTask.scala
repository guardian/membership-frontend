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

  private implicit val system = Akka.system
  val agent = Agent[T](initialValue)

  def refresh(): Future[T]

  def start() {
    Logger.debug(s"Starting ${getClass.getSimpleName} scheduled task")
    system.scheduler.schedule(initialDelay, interval) {
      agent.sendOff { _ =>
        Logger.debug(s"Refreshing ${getClass.getSimpleName} scheduled task")
        Await.result(refresh(), 15.seconds)
      }
    }
  }
}
