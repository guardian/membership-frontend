package model

import actions.GuardianDomains

case class StaffEmails(googleEmail: String, identityEmail: Option[String]) {
  def emailsMatch =
    identityEmail.exists(GuardianDomains.emailsMatch(googleEmail, _))
}
