package model

import model.EmbedSerializer._
import model.EventbriteDeserializer._
import play.api.test.PlaySpecification
import utils.Resource

class EventEmbedTest extends PlaySpecification {

  "EventEmbed" should {

    "produce the expected JSON output for a valid event" in {
      val embed = EmbedData(
        title = "Member Book Event",
        image = Some("https://media.guim.co.uk/7364090799a4b1725380c9f479e910989710ef48/0_222_1890_1134/500.jpg"),
        venue = Some("Grimsby"),
        location = Some("Grimsby, England"),
        price = Some("£3.14"),
        identifier = "local",
        start = "2016-03-04T03:00:00.000Z",
        end = "2016-03-04T03:30:00.000Z"
      )

      val jsonExpected = Resource.getJson("model/embed/expected.json")
      val jsonActual = eventToJson(Some(embed))

      jsonActual === jsonExpected
    }

    "give an error status in JSON for a nonexistent event" in {
      val jsonActual = eventToJson(None)
      (jsonActual \ "status").as[String] === "error"
    }

  }

}
