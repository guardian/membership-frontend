package controllers

import play.api.mvc._

object Signup extends Controller {

  def renderTierSelection = Action {
    Ok(views.html.signup.tierSelection())
  }

  def renderPartnerRegistration = Action {
    Ok(views.html.signup.partnerRegistration())
  }

  def renderRegistrationSuccessful = Action {
    Ok(views.html.signup.registrationSuccessful())
  }
}
