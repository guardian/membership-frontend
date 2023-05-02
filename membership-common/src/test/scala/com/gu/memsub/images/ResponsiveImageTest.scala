package com.gu.memsub.images
import io.lemonlabs.uri.Uri
import org.specs2.mutable.Specification
import io.lemonlabs.uri.dsl._

class ResponsiveImageTest extends Specification {

  val largeImage: Uri = "https://media.guim.co.uk/bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683/1000.jpg"
  val mediumImage: Uri= "https://media.guim.co.uk/bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683/500.jpg"
  val smallImage: Uri = "https://media.guim.co.uk/bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683/140.jpg"

  "ResponsiveImageGroup" should {

    "generate a src and srcset information for a sequence of images" in {

      val imageGroup = ResponsiveImageGroup(
        altText=None,
        availableImages = Seq(
          ResponsiveImage(
            path=largeImage,
            width=1000
          ),
          ResponsiveImage(
            path=mediumImage,
            width=500
          ),
          ResponsiveImage(
            path=smallImage,
            width=140
          )
        )
      )

      imageGroup.smallestImage mustEqual smallImage
      imageGroup.defaultImage mustEqual mediumImage
      imageGroup.srcset mustEqual List(
        smallImage + " 140w",
        mediumImage + " 500w",
        largeImage + " 1000w"
      ).mkString(", ")

    }

  }

  "ResponsiveImageGenerator" should {

    "generate a src and srcset information for a generated image group" in {

      val imageGroup = ResponsiveImageGroup(
        name=None,
        altText=None,
        availableImages=ResponsiveImageGenerator(
          id="bb922fe62efbe24af2df336dd2b621c5799246b4/0_0_1140_683",
          sizes=List(1000,500,140)
        )
      )

      imageGroup.smallestImage.toString mustEqual smallImage.toString
      imageGroup.defaultImage.toString mustEqual mediumImage.toString
      imageGroup.srcset mustEqual List(
        smallImage.toString + " 140w",
        mediumImage.toString + " 500w",
        largeImage.toString + " 1000w"
      ).mkString(", ")

    }

  }

}
