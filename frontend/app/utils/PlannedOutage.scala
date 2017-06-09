package utils

import org.joda.time.DateTimeZone.UTC
import org.joda.time.{DateTime, DateTimeZone, Interval}


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

  /*
  Salesforce is performing their Summer '17 Major Release on Friday 9 June 2017 at 10pm BST.
  The maintenance window is expected to last 5 minutes. Typical releases like this usually result in one minute's downtime.
  https://status.salesforce.com/status/maintenances/22710
   */
  private val BST = DateTimeZone.forID("Europe/London")
  val salesforceUpgrade = new Interval(
    new DateTime(2017, 6, 9, 22, 0, BST),
    new DateTime(2017, 6, 9, 22, 5, BST)
  )

  val outages = Seq(
    // rightNow,
    salesforceOutage,
    salesforceUpgrade
  ).sortBy(_.getStartMillis)

  def currentOutage = outages.find(_.containsNow)

  def nextOrCurrentOutage = outages.find(_.getEnd.isAfterNow)
}
