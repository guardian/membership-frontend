package services

import org.specs2.mutable.Specification

class EventIdExtractorTest extends Specification {
  "EventIdExtractor" should {
    "extract eventId from event buy url" in {
      EventIdExtractor("/event/0123456/buy") mustEqual Some("0123456")
    }

    "extract eventId from event details url" in {
      EventIdExtractor("/event/0123456") mustEqual Some("0123456")
    }

    "not extract eventId from homepage" in {
      EventIdExtractor("") mustEqual None
      EventIdExtractor("/") mustEqual None
    }

    "not extract eventId from join url" in {
      EventIdExtractor("/join/partner") mustEqual None
    }
  }
}
