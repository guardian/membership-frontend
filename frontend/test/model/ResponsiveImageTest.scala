package model

import play.api.test.PlaySpecification

class ResponsiveImageTest extends PlaySpecification {

  "ResponsiveImageGroup" should {
    "generate a srcset string for a sequence of images" in {
      val imageGroup = ResponsiveImageGroup(altText="Guardian Live event: Pussy Riot - art, sex and disobedience",
        availableImages = Seq(
          ResponsiveImage(
            path="https://media.guim.co.uk/eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368/1000.jpg",
            width=1000
          ),
          ResponsiveImage(
            path="https://media.guim.co.uk/eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368/500.jpg",
            width=500
          )
        )
      )

      imageGroup.srcset mustEqual "https://media.guim.co.uk/eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368/1000.jpg 1000w, https://media.guim.co.uk/eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368/500.jpg 500w"
    }
  }

}
