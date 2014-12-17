package actions

object GuardianDomains {

  def emailsMatch(guardianEmail: String, email: String) = {
    val emailName = guardianEmail.split("@").head.toLowerCase
    val validGuardianEmails = Seq("guardian.co.uk", "theguardian.com").map(domain => s"$emailName@$domain")
    validGuardianEmails.contains(email)
  }
}