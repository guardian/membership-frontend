package services

import com.gu.contentapi.client.model.v1._
import org.specs2.mutable.Specification
import services.MasterclassDataExtractor.extractEventbriteInformation

class MasterclassDataExtractorTest extends Specification {

  val body = "employers and educators to connect, collaborate and grow together.</p><h2><a id=\"book\">Book now</a></h2>" +
    "<p><br /><strong>If you're using a mobile device, <a href=\"https://www.eventbrite.co.uk/e/the-essentials-of-creativity-in-business-tickets-13906168725?ref=etck\">" +
    "click here to book</a></strong></p><h2>Details</h2><p><strong>Date:</strong> Tuesday 25 November 2014<br /><strong>Times:</strong> " +
    "6.30pm-9.30pm. Check-in begins 30 minutes before the start time.<br />"
  val fields = ContentFields(body = Some(body))
  val asset1 = Asset(AssetType.Image, Some("image/jpeg"), Some("main-image-file-location-1"), Some(AssetFields(width = Some(460), height = Some(276))))
  val asset2 = Asset(AssetType.Image, Some("image/jpeg"), Some("main-image-file-location-2"), Some(AssetFields(width = Some(300), height = Some(300))))
  val asset3 = Asset(AssetType.Image, Some("image/jpeg"), Some("body-image-file-location-3"), Some(AssetFields(width = Some(460), height = Some(271))))

  val mainElement = Element("gu-image-243234", "main", ElementType.Image, None, List(asset1, asset2))
  val thumbnailElement = Element("gu-image-233", "thumbnail", ElementType.Image, None, List(asset3))

  val item = Content(
   "guardian-masterclasses/a-writing-course",
   webTitle="a writing course title", webUrl="writing-gu-url", apiUrl="writing-api-url",
    fields = Some(fields),
    elements = Some(List(mainElement, thumbnailElement))
  )

  val itemWithoutBody = item.copy(fields = Some(ContentFields(body = Some("no eventrbite url"))))

  "MasterclassDataExtractor" should {

    "create a masterclass content with eventId, apiUrl, image locations of just the main element" in {
      val masterclassesContent = extractEventbriteInformation(item)
      masterclassesContent.size mustEqual(1)
      val masterclassContent = masterclassesContent(0)
      masterclassContent.eventId mustEqual("13906168725")
      masterclassContent.webUrl mustEqual("writing-gu-url")
    }

    "create multiple masterclass content for multiple eventbrite urls" in {
      val eventbriteUrlsInBody = body + "some more text about the article <a href=\"http://www.eventbrite.co.uk/e/the-essentials-of-creativity-in-business-tickets-1234\"> more" +
        "great things about the course"
      val newItem = item.copy(fields = Some(ContentFields(body = Some(eventbriteUrlsInBody))))

      val masterclassesContent = extractEventbriteInformation(newItem)
      masterclassesContent.size mustEqual(2)
      masterclassesContent.map(_.eventId) must contain(exactly("13906168725", "1234"))
      masterclassesContent.map(_.webUrl) must contain(exactly("writing-gu-url", "writing-gu-url"))

    }

    "create masterclass content if there is an eventbrite external reference" in {
      val itemWithExternalRef = itemWithoutBody.copy(references = Seq(
        Reference(`type` = "eventbrite",id = "eventbrite/111"),
        Reference(`type` = "sausages",id = "eventbrite/222"),
        Reference(`type` = "eventbrite",id = "eventbrite/333")
      ))

      val masterclassesContent = extractEventbriteInformation(itemWithExternalRef)
      masterclassesContent.map(_.eventId) must contain(exactly("111", "333"))
    }

    "create masterclass content including the eventbrite external reference plus what gets scraped" in {
      val itemWithExternalRefAndEBUrlInBody = item.copy(references = List(Reference(`type` = "eventbrite",id = "eventbrite/111")))

      val masterclassesContent = extractEventbriteInformation(itemWithExternalRefAndEBUrlInBody)
      masterclassesContent.map(_.eventId) must contain(exactly("111","13906168725"))
    }

    "not create a masterclass content if there is no eventbrite external ref and body does not contain eventbrite url" in {
      val masterclassesContent = extractEventbriteInformation(itemWithoutBody)
      masterclassesContent.size mustEqual(0)
    }

  }
}
