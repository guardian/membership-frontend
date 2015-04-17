package actions

import configuration.Config.GuardianGoogleAppsDomain

object GuardianDomains {

  def emailsMatch(guardianEmail: String, email: String) = {
    val emailName = guardianEmail.split("@").head.toLowerCase
    val validGuardianEmails = Seq(GuardianGoogleAppsDomain, "theguardian.com").map(domain => s"$emailName@$domain")
    validGuardianEmails.contains(email)
  }
}
