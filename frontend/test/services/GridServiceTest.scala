package services

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import model.Grid.GridResult
import model.GridDeserializer._
import org.specs2.mutable.Specification
import services.GridService.ImageIdWithCrop.fromGuToolsUri
import utils.Resource

class GridServiceTest extends Specification {
  import GridService.ImageIdWithCrop

  val validGridUrl: Uri = "https://media.gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288"

  "ImageIdWithCrop" should {
    "build only from valid urls" in {
      fromGuToolsUri(validGridUrl) must beSome(ImageIdWithCrop("aef2fb1db22f7cd20683548719a2849b3c9962ec", "0_19_480_288"))
      fromGuToolsUri("https://twitter.com/rtyley") must beNone
    }
  }

  "GridService" should {
    "cropParam" in {
      GridService.cropParam(validGridUrl) mustEqual Some("0_19_480_288")
      GridService.cropParam("http://example.com?q=v") mustEqual None
    }
    "must return requested crop with dimensions" in {
      val grid = Resource.getJson("model/grid/api-image.json")
      val gridResponse = grid.as[GridResult]
      val exports = gridResponse.data.exports.get
      val requestedCrop = "0_130_1703_1022"
      val assets = GridService.findAssets(exports, requestedCrop)

      assets.size mustEqual(3)
      assets.map(_.dimensions.width) mustEqual(List(1000, 500, 140))
    }
  }
}
