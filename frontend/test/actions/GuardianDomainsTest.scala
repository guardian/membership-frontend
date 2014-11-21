package actions

import org.specs2.mutable.Specification

class GuardianDomainsTest extends Specification {

  "isValidGuardianEmail" should {
    "be a valid Guardian email" in {
      GuardianDomains.emailsMatch("joe.bloggs@guardian.co.uk", "joe.bloggs@theguardian.com") mustEqual(true)
      GuardianDomains.emailsMatch("joe.bloggs@theguardian.com", "joe.bloggs@theguardian.com") mustEqual(true)

      GuardianDomains.emailsMatch("joe.bloggs@guardian.co.uk", "joe.bloggs@gmail.com") mustEqual(false)
      GuardianDomains.emailsMatch("joe.bloggssssss@theguardian.com", "joe.bloggs@theguardian.com") mustEqual(false)
    }
  }
}