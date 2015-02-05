package model

import org.specs2.mutable.Specification
import utils.Resource
import model.CAS._
import model.CAS.Deserializer._

class CASDeserializerTest extends Specification {
  "CASDeserializer" should {
    "deserialize a success" in {
      val successOpt = Resource.getJson("model/cas/success.json").asOpt[CASSuccess]
      successOpt mustEqual Some(CASSuccess("sub", "provider", "2015-02-24", "XXX", "CONTENT"))
    }

    "deserialize an error" in {
      val errorOpt = Resource.getJson("model/cas/error.json").asOpt[CASError]
      errorOpt mustEqual Some(CASError("Unknown subscriber", -90))
    }
  }

}
