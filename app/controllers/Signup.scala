package controllers

import play.api.mvc._
import org.joda.time.DateTime

object Signup extends Controller {

  def renderTierSelection = Action {
    Ok(views.html.signup.tierSelection())
  }

  def renderPartnerRegistration = Action {
    val thisYear = DateTime.now().year().get()
    val validCardYears = thisYear until (thisYear + 20)

    val validCardMonths = 1 until 13
    Ok(views.html.signup.partnerRegistration(validCardMonths.toList, validCardYears.toList))
  }

  def renderRegistrationSuccessful = Action {
    Ok(views.html.signup.registrationSuccessful())
  }

}
