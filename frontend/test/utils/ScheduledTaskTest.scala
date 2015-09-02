package utils

import org.specs2.mutable.Specification
import play.api.test.FakeApplication
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.duration._

class ScheduledTaskTest extends Specification {

  "Scheduled Task" should {
    "update value on refresh" in {
      running(FakeApplication()) {
        "A" mustNotEqual "B"
        val random = scala.util.Random
        val task = ScheduledTask[Int]("Random number scheduled task", 0, 1.second, 1.second) {
          Future.successful(random.nextInt())
        }
        task.start()
        val first: Int = task.get()
        Thread.sleep(1500);
        first mustNotEqual (task.get())
      }
    }
  }
}
