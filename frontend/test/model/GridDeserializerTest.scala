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

      gridResponse.uri mustEqual("https://api.media.test.dev-gutools.co.uk/images/dff117cdb2100899107c96d7933ef175c35765ca")
      gridResponse.data.metadata.byline mustEqual("David Goldman")

      val exports = gridResponse.data.exports
      exports.size mustEqual(1)
      exports(0).assets.size mustEqual(4)
      val asset1 = exports(0).assets(0)
      asset1.file mustEqual("http://media-origin.test.dev-guim.co.uk/dff117cdb2100899107c96d7933ef175c35765ca/679_255_4505_2704/2000.jpg")
      asset1.secureFile should be (None)
      asset1.dimensions.height mustEqual (1200)
    }
  }

}
