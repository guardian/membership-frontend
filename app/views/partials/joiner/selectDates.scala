package views.partials.joiner

import org.joda.time.DateTime

object selectDates {
  def thisYear = DateTime.now().year().get()
  def validCardYears = thisYear until (thisYear + 20)

  def validCardMonths = 1 until 13
}

//def renderPartnerRegistration = Action {
//val thisYear = DateTime.now().year().get()
//val validCardYears = thisYear until (thisYear + 20)
//
//val validCardMonths = 1 until 13
//Ok(views.html.signup.partnerRegistration(validCardMonths.toList, validCardYears.toList))
