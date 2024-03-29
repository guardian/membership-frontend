package model

import configuration.Links

object EventMetadata {

  case class Metadata(
    identifier: String,
    title: String,
    shortTitle: String,
    pluralTitle: String,
    description: Option[String],
    socialHashtag: Option[String],
    eventListUrl: String,
    termsUrl: String,
    highlightsOpt: Option[HighlightsMetadata] = None,
    chooseTier: ChooseTierMetadata,
    brand: String
  )

  case class ChooseTierMetadata(title: String, sectionTitle: String)
  case class HighlightsMetadata(title: String, url: String)

  val liveMetadata = Metadata(
    identifier="guardian-live",
    title="Guardian Live events",
    shortTitle="Events",
    pluralTitle="Guardian Live events",
    description=Some("""
      |Guardian Live is a programme of discussions, debates, interviews, keynote speeches and festivals.
      |Members can attend events that take the power of open journalism from print and digital into live experiences.
    """.stripMargin),
    socialHashtag=Some("#GuardianLive"),
    eventListUrl=controllers.routes.WhatsOn.list().url,
    termsUrl=Links.guardianLiveTerms,
    chooseTier=ChooseTierMetadata(
      "Guardian Live events are exclusively for Guardian members",
      "Choose a membership tier to continue with your booking"
    ),
    brand = "A Guardian Live event"
  )

  val masterclassMetadata = Metadata(
    identifier="masterclasses",
    title="Guardian Masterclasses",
    shortTitle="Masterclasses",
    pluralTitle="Masterclasses",
    description=Some("""
      |Guardian Masterclasses offer a broad range of short and long courses across a variety of disciplines from creative writing,
      | journalism, photography and design, film and digital media, music and cultural appreciation.
    """.stripMargin),
    socialHashtag=Some("#GuardianMasterclasses"),
    eventListUrl=controllers.routes.WhatsOn.masterclassesList().url,
    termsUrl=Links.guardianMasterclassesTerms,
    chooseTier=ChooseTierMetadata(
      "Choose a membership tier to continue with your booking",
      "Become a Partner or Patron to save 20% on your masterclass"
    ),
    brand = ""
  )

}
