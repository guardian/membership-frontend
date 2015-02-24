package services

import model.Grid.GridResult
import org.specs2.mutable.Specification
import utils.Resource
import model.GridDeserializer._
import com.netaporter.uri.dsl._

class GridServiceTest extends Specification {

  val configUrl = "https://valid-media-tool-url/images/"
  val validGridUrl = "https://valid-media-tool-url/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"
  val invalidGridUrl = "https://invalid-media-tool-url/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"

  "GridService" should {
     val service = new GridService(configUrl)

    "confirm the url supplied in Eventbrite is from the Grid" in {
      service.isUrlCorrectFormat(validGridUrl) mustEqual (true)
    }

    "confirm the url supplied in Evenbtrite is not from the Grid" in {
      service.isUrlCorrectFormat(invalidGridUrl) mustEqual (false)
    }

    "must get endpoint for a Grid url" in {
      service.getEndpoint(validGridUrl) mustEqual("aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288")
    }
    "must not get an endpoint for invalid Grid url" in {
      service.getEndpoint(invalidGridUrl) mustEqual(invalidGridUrl)
    }

    "must return the crop parameters from Grid url" in {
      service.cropParam(validGridUrl) mustEqual(Some("0_19_480_288"))
    }

    "must return None if no parameter is passed into url" in {
      service.cropParam("https://media.test.dev-gutools.co.uk/images/fsifjsifjsi") mustEqual(None)
    }

    "must return requested crop with dimensions" in {
      val grid = Resource.getJson("model/grid/api-image.json")
      val gridResponse = grid.as[GridResult]
      val exports = gridResponse.data.exports.get
      val requestedCrop = Some("0_130_1703_1022")
      val assets = service.findAssets(exports, requestedCrop)

      assets.size mustEqual(3)
      assets.map(_.dimensions.width) mustEqual(List(1000, 500, 140))
    }

    "must return first crop when requested crop is not defined" in {
      val grid = Resource.getJson("model/grid/api-image.json")
      val gridResponse = grid.as[GridResult]
      val exports = gridResponse.data.exports.get

      val assets = service.findAssets(exports, None)

      assets.size mustEqual(2)
      assets.map(_.dimensions.width) mustEqual(List(1000, 500))
    }
  }
}