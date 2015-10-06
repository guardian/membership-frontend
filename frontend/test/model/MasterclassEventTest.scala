package model

import org.specs2.mutable.Specification

import Eventbrite.EBEvent
import RichEvent.MasterclassEvent
import EventbriteDeserializer._
import utils.Resource

class MasterclassEventTest extends Specification {
  "extractTags" should {
    "extract tags from within a body of text" in {
      val body =
        """
          |This is some test body of text, the extractor should be able
          |to <!-- tags: a,b,c --> extract these tags
        """.stripMargin
      MasterclassEvent.extractTags(body) mustEqual Some(Seq("a", "b", "c"))
    }
    "only match tags within a comment" in {
      MasterclassEvent.extractTags("blah <!-- tags: a,b --> foo bar tags: a,b -->") mustEqual Some(Seq("a", "b"))
      MasterclassEvent.extractTags("tags: a -->") must beNone
      MasterclassEvent.extractTags("<!-- tags: a") must beNone
    }
    "extract a number of tags" in {
      MasterclassEvent.extractTags("<!-- tags: a,b,c,d -->") mustEqual Some(Seq("a", "b", "c", "d"))
    }
    "be lenient about white space" in {
      MasterclassEvent.extractTags("<!-- tags: a  -->") mustEqual Some(Seq("a"))
      MasterclassEvent.extractTags("<!--tags: a,b  ,c  ,d-->") mustEqual Some(Seq("a", "b", "c", "d"))
    }
    "match tags with white space" in {
      MasterclassEvent.extractTags("<!-- tags: Copy writing, Data visualisation, Food -->") mustEqual Some(Seq("copy writing", "data visualisation", "food"))
    }
  }

  "MasterclassEvent" should {
    "extract tags when they are present" in {
      val event = Resource.getJson("model/eventbrite/event-with-tag.json").as[EBEvent]
      val mcEvent = MasterclassEvent(event, None, None)

      mcEvent.tags mustEqual Seq("writing", "creative writing", "genre writing")
    }

    "return an empty list when tags are not present" in {
      val event = Resource.getJson("model/eventbrite/event-without-tag.json").as[EBEvent]
      val mcEvent = MasterclassEvent(event, None, None)

      mcEvent.tags mustEqual Nil
    }
  }
}
