package model

import com.gu.memsub.images.Grid.GridResult
import play.api.test.PlaySpecification
import utils.Resource
import com.gu.memsub.images.GridDeserializer._

class GridDeserializerTest extends PlaySpecification {

  "GridDeserializerTest" should {

    "deserializer grid json" in {
      val grid = Resource.getJson("model/grid/api-image.json")
      val gridResponse = grid.as[GridResult]

      gridResponse.data.metadata.byline mustEqual(Some("Joe Bloggs"))

      val exports = gridResponse.data.exports.get
      exports.size mustEqual(2)
      exports(0).assets.size mustEqual(2)
      val asset1 = exports(0).assets(0)
      asset1.file mustEqual("http://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/1000.jpg")
      asset1.secureUrl mustEqual(Some("https://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/1000.jpg"))
      asset1.dimensions.height mustEqual (600)
    }

    "deserialize grid that has no exports" in {
      val grid = Resource.getJson("model/grid/api-image-no-exports.json")
      val gridResponse = grid.as[GridResult]

      val exports = gridResponse.data.exports
      exports must beNone

    }
  }

}
