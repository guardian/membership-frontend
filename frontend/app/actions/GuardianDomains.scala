package actions

import com.gu.memsub.auth.common.MemSub.Google.GuardianAppsDomain

object GuardianDomains {

  def emailsMatch(guardianEmail: String, email: String) = {
    val emailName = guardianEmail.split("@").head.toLowerCase
    val validGuardianEmails = Seq(GuardianAppsDomain, "theguardian.com").map(domain => s"$emailName@$domain")
    validGuardianEmails.contains(email)
  }
}
