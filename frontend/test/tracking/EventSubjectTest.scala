package tracking

import com.gu.membership.salesforce.Tier
import forms.MemberForm.MarketingChoicesForm
import org.specs2.mutable.Specification


class EventSubjectTest extends Specification {

  val eventSubject = EventSubject("salesforce123", "identity123", "Partner",
    Some(DowngradeAmendment(Tier.Partner)), Some("N1 9GU"), None, Some(true), Some(MarketingChoicesForm(Some(true), Some(false))))

  "EventSubject" should {
    "create a valid JMap for tracking" in {

      val eventSubjectAsMap = eventSubject.toMap
      eventSubjectAsMap.get("salesforceContactId") !=  contain("salesforce123")
      eventSubjectAsMap.get("identityId") !=  contain("identity123")
      eventSubjectAsMap.get("deliveryPostcode") mustEqual "N1"
      eventSubjectAsMap.get("amendTier").toString mustEqual("{from=Partner, to=Friend}")

    }

    "truncate postcode" in {
      eventSubject.truncatePostcode("N1 9GU") mustEqual "N1"
      eventSubject.truncatePostcode("N19GU") mustEqual "N1"

      eventSubject.truncatePostcode("N12 9GU") mustEqual "N12"
      eventSubject.truncatePostcode("N129GU") mustEqual "N12"

      eventSubject.truncatePostcode("NW12 9GU") mustEqual "NW12"
      eventSubject.truncatePostcode("NW129GU") mustEqual "NW12"
    }
  }
}
