package model

import actions.GuardianDomains

case class StaffEmails(googleEmail: String, identityEmail: Option[String]) {
  def emailsMatch = identityEmail.map(GuardianDomains.emailsMatch(googleEmail, _)).getOrElse(false)
}
