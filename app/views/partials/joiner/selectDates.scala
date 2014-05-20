package views.partials.joiner

import org.joda.time.DateTime

object selectDates {
  def thisYear = DateTime.now().year().get()
  def validCardYears = thisYear until (thisYear + 20)

  def validCardMonths = 1 until 13
}
