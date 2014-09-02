package model

import org.specs2.mutable.Specification
import model.ZuoraDeserializer._
import utils.Resource

class ZuoraDeserializerTest extends Specification {
  "Zuora" should {
    "deserialize query with no results" in {
      val query = queryReader.read(Resource.getXML("model/zuora/query-empty.xml"))
      query.results.size mustEqual 0
    }

    "deserialize query with one result" in {
      val query = queryReader.read(Resource.getXML("model/zuora/query-single.xml"))

      query.results.size mustEqual 1

      query.results(0)("Id") mustEqual "2c92c0f947cddc220147d3c765d0433e"
      query.results(0)("Version") mustEqual "1"
    }

    "deserialize query with multiple results" in {
      val query = queryReader.read(Resource.getXML("model/zuora/subscriptions.xml"))

      query.results.size mustEqual 3

      query.results(0)("Id") mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      query.results(0)("Version") mustEqual "1"

      query.results(1)("Id") mustEqual "2c92c0f847cdc31e0147cf243a166af0"
      query.results(1)("Version") mustEqual "3"

      query.results(2)("Id") mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      query.results(2)("Version") mustEqual "2"
    }
  }
}
