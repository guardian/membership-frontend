package services

import model.Grid.GridResult
import org.specs2.mutable.Specification
import utils.Resource
import model.GridDeserializer._
import com.netaporter.uri.dsl._

class GridServiceTest extends Specification {
  import GridService.ImageIdWithCrop

  val configUrl = "https://valid-media-tool-url/images/"
  val validGridUrl = "https://valid-media-tool-url/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"
  val invalidGridUrl = "https://invalid-media-tool-url/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"

  "ImageIdWithCrop" should {
    "build only from valid urls" in {
      val imageIdWithCrop = ImageIdWithCrop.fromGuToolsUri(configUrl) _
      imageIdWithCrop(validGridUrl) mustEqual Some(ImageIdWithCrop("aef2fb1db22f7cd20683548719a2849b3c9962ec", "0_19_480_288"))
      imageIdWithCrop(invalidGridUrl) mustEqual None
    }
  }

  "GridService" should {
    val service = new GridService(configUrl)

    "cropParam" in {
      GridService.cropParam(validGridUrl) mustEqual Some("0_19_480_288")
      GridService.cropParam("http://example.com?q=v") mustEqual None
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
