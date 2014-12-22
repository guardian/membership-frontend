package services

import org.specs2.mutable.Specification


class GridServiceTest extends Specification {
  val service = new GridService

  "GridService" should {
    "confirm the url supplied in Eventbrite is from the Grid" in {
      service.isUrlCorrectFormat("https://media.test.dev-gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288") mustEqual (true)
    }

//    "confirm the url supplied in Evenbtrite is not from the Grid" in {
//      service.isUrlCorrectFormat("https://incorrect-format.media.test.dev-gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288") mustEqual (false)
//    }
  }

}
