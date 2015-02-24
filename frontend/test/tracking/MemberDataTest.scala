package tracking

import com.gu.membership.salesforce.Tier
import forms.MemberForm.MarketingChoicesForm
import org.specs2.mutable.Specification


class MemberDataTest extends Specification {

  val memberData = MemberData("salesforce123", "identity123", "Partner",
    Some(DowngradeAmendment(Tier.Partner)), Some("N1 9GU"), None, Some(true), Some(MarketingChoicesForm(Some(true), Some(false))))

  "MemberData" should {
    "create a valid JMap for tracking" in {

      val eventSubjectAsMap = memberData.toMap
      eventSubjectAsMap.get("salesforceContactId") !=  contain("salesforce123")
      eventSubjectAsMap.get("identityId") !=  contain("identity123")
      eventSubjectAsMap.get("deliveryPostcode") mustEqual "N1"
      eventSubjectAsMap.get("amendTier").toString mustEqual("{from=Partner, to=Friend}")

    }

    "truncate postcode" in {
      memberData.truncatePostcode("N1 9GU") mustEqual "N1"
      memberData.truncatePostcode("N19GU") mustEqual "N1"

      memberData.truncatePostcode("N12 9GU") mustEqual "N12"
      memberData.truncatePostcode("N129GU") mustEqual "N12"

      memberData.truncatePostcode("NW12 9GU") mustEqual "NW12"
      memberData.truncatePostcode("NW129GU") mustEqual "NW12"
    }
  }
}
