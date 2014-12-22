package services

import org.specs2.mutable.Specification


class GridServiceTest extends Specification {
  val service = new GridService

  val validGridUrl = "https://media.test.dev-gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"
  val invalidGridUrl = "https://incorrect-format.media.test.dev-gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"

  "GridService" should {
    "confirm the url supplied in Eventbrite is from the Grid" in {
      service.isUrlCorrectFormat(validGridUrl) mustEqual (true)
    }

    "confirm the url supplied in Evenbtrite is not from the Grid" in {
      service.isUrlCorrectFormat(invalidGridUrl) mustEqual (false)
    }

    "must substitute a Grid url with it's API equivalent" in {
      service.getApiUrl(validGridUrl) mustEqual("https://api.media.test.dev-gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288")
    }
  }

  "must not substitute a Grid url with it's API equivalent" in {
      service.getApiUrl(invalidGridUrl) mustEqual(invalidGridUrl)
  }

  "must return the crop parameters from Grid url" in {
    service.getCropRequested(validGridUrl) mustEqual(Some("0_19_480_288"))
  }

  "must return None if no parameter is passed into url" in {
    service.getCropRequested("https://media.test.dev-gutools.co.uk/images/fsifjsifjsi") mustEqual(None)
  }
}
