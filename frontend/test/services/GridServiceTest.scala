package services

import io.lemonlabs.uri.{Uri, Url}
import io.lemonlabs.uri.typesafe.dsl._
import com.gu.memsub.images.Grid.GridResult
import com.gu.memsub.images.GridDeserializer._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import services.GridService.ImageIdWithCrop.fromGuToolsUri
import utils.Resource

class GridServiceTest(implicit ev: ExecutionEnv) extends Specification {
  import GridService.ImageIdWithCrop

  val validGridUrl: Url = "https://media.gutools.co.uk/images/aef2fb1db22f7cd20683548719a2849b3c9962ec?crop=0_19_480_288".toUrl

  "ImageIdWithCrop" should {
    "build only from valid urls" in {
      fromGuToolsUri(validGridUrl) must beSome(ImageIdWithCrop("aef2fb1db22f7cd20683548719a2849b3c9962ec", "0_19_480_288"))
      fromGuToolsUri("https://twitter.com/rtyley") must beNone
    }
  }

  "GridService" should {
    "must return requested crop with dimensions" in {
      val grid = Resource.getJson("model/grid/api-image.json")
      val gridResponse = grid.as[GridResult]
      val exports = gridResponse.data.exports.get
      val requestedCrop = "0_130_1703_1022"
      val gridService = new GridService(ev.executionContext)
      val `export` = gridService.findExport(`exports`, requestedCrop)

      export must beSome
      export.get.assets.size mustEqual(3)
      export.get.assets.map(_.dimensions.width) mustEqual(List(1000, 500, 140))
    }
  }
}
