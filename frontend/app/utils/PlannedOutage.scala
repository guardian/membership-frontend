package utils

import org.joda.time.DateTimeZone.UTC
import org.joda.time.{DateTime, Interval}


object PlannedOutage {

  val rightNow = new Interval(DateTime.now().minusMinutes(3), DateTime.now().plusMinutes(10))

  /*
  Salesforce EU0 instance refresh scheduled for November 6, 2016.

  The instance refresh will take place on Sunday, November 6, 2016 during the 02:00–04:45 UTC standard system maintenance window.
  Your org will be available in read-only mode for the duration of the maintenance.
 */
  val salesforceOutage = new Interval(
    new DateTime(2016,11, 6, 2, 0, UTC),
    new DateTime(2016,11, 6, 4,45, UTC)
  )

  val outages = Seq(
    // rightNow,
    salesforceOutage
  ).sortBy(_.getStartMillis)

  def currentOutage = outages.find(_.containsNow)

  def nextOrCurrentOutage = outages.find(_.getEnd.isAfterNow)
}
