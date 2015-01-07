package model

import configuration.Config
import model.Eventbrite._
import model.Grid.{Metadata, GridResult}
import model.GridDeserializer._
import org.specs2.mock.Mockito
import play.api.test.PlaySpecification
import utils.Resource

class GuLiveEventTest extends PlaySpecification with Mockito {

  val event = mock[EBEvent]
  val grid = Resource.getJson("model/grid/api-image.json")
  val gridResponse = grid.as[GridResult]

  "GuLiveEventTest" should {
    "use secure url for image" in {
      val image = EventImage(gridResponse.data.exports(0).assets, gridResponse.data.metadata)
      val guEvent = GuLiveEvent(event, Some(image))

      guEvent.imgUrl mustEqual ("https://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/1000.jpg")
      guEvent.imageMetadata.flatMap(_.description) mustEqual(Some("It's Chris!"))
      guEvent.imageMetadata.flatMap(_.source) mustEqual(None)
      guEvent.availableWidths mustEqual(List(1000, 500))
    }

    "use file url for image when no secure url is present" in {
      val image = EventImage(gridResponse.data.exports(1).assets, gridResponse.data.metadata)
      val guEvent = GuLiveEvent(event, Some(image))

      guEvent.imgUrl mustEqual ("http://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/0_130_1703_1022/1000.jpg")
      guEvent.imageMetadata.flatMap(_.description) mustEqual(Some("It's Chris!"))
      guEvent.imageMetadata.flatMap(_.source) mustEqual(None)
      guEvent.availableWidths mustEqual(List(1000, 500, 140))
    }

    "use fallback image when no image is found from the Grid" in {
      val guEvent = GuLiveEvent(event, None)

      guEvent.imgUrl mustEqual(Config.gridConfig.fallbackImageUrl)
      guEvent.imageMetadata.flatMap(_.description) mustEqual(None)
      guEvent.imageMetadata.flatMap(_.source) mustEqual(None)
      guEvent.availableWidths mustEqual(List.empty)
    }

    "use fallback image when assets in export is an empty list from the Grid" in {
      val image = EventImage(Nil, Metadata(None, None, None, None))
      val guEvent = GuLiveEvent(event, Some(image))

      guEvent.imgUrl mustEqual(Config.gridConfig.fallbackImageUrl)
      guEvent.imageMetadata.flatMap(_.description) mustEqual(None)
      guEvent.imageMetadata.flatMap(_.source) mustEqual(None)
      guEvent.availableWidths mustEqual(List.empty)

    }
  }
}
