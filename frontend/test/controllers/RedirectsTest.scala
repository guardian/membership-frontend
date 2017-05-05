package controllers

import com.gu.i18n.CountryGroup
import org.specs2.mutable.Specification
import Redirects._

class RedirectsTest extends Specification {
  "redirectToSupporterPage" in {
    redirectToSupporterPage(CountryGroup.UK) must_=== routes.Info.supporterFor(CountryGroup.UK)
    redirectToSupporterPage(CountryGroup.US) must_=== routes.Info.supporterUSA()
    redirectToSupporterPage(CountryGroup.Europe) must_=== routes.Info.supporterFor(CountryGroup.Europe)
    redirectToSupporterPage(CountryGroup.Australia) must_=== routes.Info.supporterAustralia()
    redirectToSupporterPage(CountryGroup.Canada) must_=== routes.Info.supporterFor(CountryGroup.Canada)
    redirectToSupporterPage(CountryGroup.RestOfTheWorld) must_=== routes.Info.supporterFor(CountryGroup.RestOfTheWorld)
  }
}
