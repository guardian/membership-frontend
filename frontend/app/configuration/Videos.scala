package configuration

import com.gu.memsub.images.{ResponsiveImageGenerator, ResponsiveImageGroup}
import com.netaporter.uri.dsl._
import model.Video

object Videos {

  private val supportersPlaceholder = ResponsiveImageGenerator(
    id="9dc72e35ca1a38f6c02933d48f12de863739ad60/125_0_1873_1124",
    sizes=List(1000, 500)
  )

  private val scottTrustPlaceholder = ResponsiveImageGenerator(
    id="82dbcf960aa2dd780b51ec2829fa3a15304cf921/0_0_2000_1125",
    sizes=List(2000, 500)
  )

  private val membershipPlaceholder = ResponsiveImageGenerator(
    id="d6e58ef1af3c7f06477c1f0709c823613bc21f3e/0_0_2000_1125",
    sizes=List(2000, 500)
  )

  private val supportersUSAPlaceholder = ResponsiveImageGenerator(
    id="d9e217dbfa10f4c6b352ff2abac13379994a992f/0_0_960_540",
    sizes=List(960, 500)
  )

  private val supportersAUPlaceholder = ResponsiveImageGenerator(
    id="700dda3ef042336defa067413cba6069b1a97811/0_0_1920_1080",
    sizes=List(1920,960,500)
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

  val scottTrustExplained = Video(
    srcUrl="//www.youtube.com/embed/jn4wAy1Xs5M?enablejsapi=1&wmode=transparent&rel=0",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=scottTrustPlaceholder
      )
    )
  )

  val membershipExplained = Video(
    srcUrl="//www.youtube.com/embed/E3uaUH2XGtE?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=membershipPlaceholder
      )
    ),
    autoplay=true,
    loop=true
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

  val supportersAU = Video(
    srcUrl = "https://www.youtube.com/embed/OIHC_zxU9c4?enablejsapi=1&wmode=transparent",
    posterImage = Some(
      ResponsiveImageGroup(
        altText=Some("Support the Guardian"),
        availableImages=supportersAUPlaceholder
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
