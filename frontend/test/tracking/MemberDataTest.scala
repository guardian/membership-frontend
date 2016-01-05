package tracking

import com.gu.salesforce.Tier.partner
import forms.MemberForm.MarketingChoicesForm
import org.specs2.mutable.Specification


class MemberDataTest extends Specification {

  val memberData = MemberData("salesforce123", "identity123", "Partner",
    Some(DowngradeAmendment(partner)), Some("N1 9GU"), None, Some(true), Some(MarketingChoicesForm(Some(true), Some(false))))

  "MemberData" should {
   
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
