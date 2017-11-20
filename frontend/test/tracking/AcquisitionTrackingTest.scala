package tracking

import actions.CommonActions
import com.gu.acquisition.model.ReferrerAcquisitionData
import ophan.thrift.componentEvent.ComponentType.AcquisitionsEpic
import ophan.thrift.event.AbTest
import ophan.thrift.event.AcquisitionSource.Social
import org.specs2.matcher.OptionMatchers
import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.api.mvc.Session

class AcquisitionTrackingTest extends Specification with OptionMatchers
  with AcquisitionTracking {

  val referrerAcquisitionData = ReferrerAcquisitionData(
    campaignCode = Some("campaign_code"),
    referrerPageviewId = Some("pvid"),
    referrerUrl = Some("askjeeves.com"),
    componentId = Some("banner1"),
    componentType = Some(AcquisitionsEpic),
    source = Some(Social),
    abTest = Some(AbTest("test1", "variant1")),
    abTests = Some(Set(AbTest("test2", "variant2"), AbTest("test3", "variant3")))
  )

  "AcquisitionTracking" should {
    "decode ReferrerAcquisitionData from Play session" in {
      val json = Json.toJson(referrerAcquisitionData)
      val session = Session(Map(CommonActions.acquisitionDataSessionKey -> json.toString))

      decodeAcquisitionData(session) should beSome(referrerAcquisitionData)
    }
  }
}
