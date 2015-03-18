package model

import model.EventbriteTestObjects._
import model.Grid.{GridResult, Metadata}
import model.GridDeserializer._
import model.RichEvent.{GuLiveEvent, GridImage}
import org.specs2.mock.Mockito
import play.api.test.PlaySpecification
import utils.Resource

class GuLiveEventTest extends PlaySpecification with Mockito {

  val event = eventWithName()
  val grid = Resource.getJson("model/grid/api-image.json")
  val gridResponse = grid.as[GridResult]

  "GuLiveEventTest" should {

    "contain metadata and socialUrl for an event image" in {

      val image = GridImage(gridResponse.data.exports.get(1).assets, gridResponse.data.metadata)
      val guEvent = GuLiveEvent(event, Some(image), None)

      guEvent.imageMetadata.flatMap(_.description) mustEqual Some("It's Chris!")
      guEvent.imageMetadata.map(_.photographer) mustEqual Some("Joe Bloggs/Guardian Images")

      guEvent.socialImgUrl.get mustEqual "http://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/0_130_1703_1022/500.jpg"
    }
  }
}
