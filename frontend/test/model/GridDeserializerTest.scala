package model

import model.Grid.GridResult
import play.api.test.PlaySpecification
import utils.Resource
import model.GridDeserializer._

class GridDeserializerTest extends PlaySpecification {

  "GridDeserializerTest" should {

    "deserializer grid json" in {
      val grid = Resource.getJson("model/grid/api-image.json")
      val gridResponse = grid.as[GridResult]

      gridResponse.uri mustEqual("https://some-media-api-service/images/aede0da05506d0d8cb993558b7eb9ad1d2d3e675")
      gridResponse.data.metadata.byline mustEqual(Some("Gu membership"))

      val exports = gridResponse.data.exports
      exports.size mustEqual(2)
      exports(0).assets.size mustEqual(2)
      val asset1 = exports(0).assets(0)
      asset1.file mustEqual("http://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/1000.jpg")
      asset1.secureFile mustEqual(Some("https://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/1000.jpg"))
      asset1.dimensions.height mustEqual (600)
    }
  }

}
