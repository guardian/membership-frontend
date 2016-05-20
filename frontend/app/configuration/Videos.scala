package configuration

import com.netaporter.uri.dsl._
import model.{Video, ResponsiveImageGroup, ResponsiveImageGenerator}

object Videos {

  private val whatIsMembershipPlaceholder = ResponsiveImageGenerator(
    id="9dc72e35ca1a38f6c02933d48f12de863739ad60/125_0_1873_1124",
    sizes=List(1000, 500)
  )
  val whatIsMembership = Video(
    srcUrl="//www.youtube.com/embed/9uV35JdlFdM?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("What is Guardian Members?"),
        availableImages=whatIsMembershipPlaceholder
      )
    )
  )

  private val supportersPlaceholder = ResponsiveImageGenerator(
    id="9dc72e35ca1a38f6c02933d48f12de863739ad60/125_0_1873_1124",
    sizes=List(1000, 500)
  )

  private val supportersUSAPlaceholder = ResponsiveImageGenerator(
    id="d9e217dbfa10f4c6b352ff2abac13379994a992f/0_0_960_540",
    sizes=List(960, 500)
  )


  val supporters = Video(
    srcUrl="//www.youtube.com/embed/rBnvGHEyATc?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=supportersPlaceholder
      )
    )
  )

  val supportersUSA =  Video(
    srcUrl="//www.youtube.com/embed/MsZL6dhyXW8?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=supportersUSAPlaceholder
      )
    )
  )

  val partners = Video(
    srcUrl="//www.youtube.com/embed/rBnvGHEyATc?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=supportersPlaceholder // TODO change to a partnersPlaceholder
      )
    )
  )

}
