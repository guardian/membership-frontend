package model

import model.Eventbrite._
import model.Grid.{GridResult, Metadata}
import model.GridDeserializer._
import model.RichEvent.{GuLiveEvent, EventImage}
import org.specs2.mock.Mockito
import play.api.test.PlaySpecification
import utils.Resource

class GuLiveEventTest extends PlaySpecification with Mockito {

  val event = mock[EBEvent]
  val grid = Resource.getJson("model/grid/api-image.json")
  val gridResponse = grid.as[GridResult]

  "GuLiveEventTest" should {
    "contain secure url, metadata and socialUrl for image" in {
      val image = EventImage(gridResponse.data.exports.get(0).assets, gridResponse.data.metadata)
      val guEvent = GuLiveEvent(event, Some(image), None)

      guEvent.imgUrl mustEqual "https://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/{width}.jpg"
      guEvent.imageMetadata.flatMap(_.description) mustEqual Some("It's Chris!")
      guEvent.imageMetadata.map(_.photographer) mustEqual Some("Joe Bloggs/Guardian Images")
      guEvent.availableWidths mustEqual "1000,500"
      guEvent.socialImgUrl mustEqual "https://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/294_26_1584_950/1000.jpg"
    }

    "use file url, metadata, socialUrl for image when no secure url is present" in {
      val image = EventImage(gridResponse.data.exports.get(1).assets, gridResponse.data.metadata)
      val guEvent = GuLiveEvent(event, Some(image), None)

      guEvent.imgUrl mustEqual "http://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/0_130_1703_1022/{width}.jpg"
      guEvent.imageMetadata.flatMap(_.description) mustEqual Some("It's Chris!")
      guEvent.imageMetadata.map(_.photographer) mustEqual Some("Joe Bloggs/Guardian Images")

      guEvent.availableWidths mustEqual "1000,500,140"
      guEvent.socialImgUrl mustEqual "http://some-media-thing/aede0da05506d0d8cb993558b7eb9ad1d2d3e675/0_130_1703_1022/1000.jpg"
    }

    "use fallback image when no image is found from the Grid" in {
      val guEvent = GuLiveEvent(event, None, None)

      guEvent.socialImgUrl must contain("event-placeholder.gif")
      guEvent.imageMetadata.flatMap(_.description) mustEqual None
      guEvent.imageMetadata.map(_.photographer) mustEqual None
      guEvent.availableWidths mustEqual ""
      guEvent.socialImgUrl must contain("event-placeholder.gif")
    }

    "use fallback image when assets in export is an empty list from the Grid" in {
      val image = EventImage(Nil, Metadata(None, None, None))
      val guEvent = GuLiveEvent(event, Some(image), None)

      guEvent.socialImgUrl must contain("event-placeholder.gif")
      guEvent.imageMetadata.flatMap(_.description) mustEqual None
      guEvent.imageMetadata.map(_.photographer).get mustEqual ""
      guEvent.availableWidths mustEqual ""
      guEvent.socialImgUrl must contain("event-placeholder.gif")
    }
  }
}
