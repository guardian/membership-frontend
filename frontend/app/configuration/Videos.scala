package configuration

import com.netaporter.uri.dsl._
import model.{Video, ResponsiveImageGroup, ResponsiveImageGenerator}

object Videos {
  val whatIsMembership = Video(
    srcUrl="//www.youtube.com/embed/7JnYthFvYEk?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="267569fecb462c61718f7e8cf50a8995ebddee5d/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      )
    )
  )
  val whatIsMembershipAlt = Video(
    srcUrl="//www.youtube.com/embed/z_IFkfuEuz0?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("What is Guardian Membership?"),
        availableImages=ResponsiveImageGenerator(
          id="267569fecb462c61718f7e8cf50a8995ebddee5d/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      )
    )
  )
  val supporters = Video(
    srcUrl="//www.youtube.com/embed/uTm3spGwpFI?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="76c306a5c0fed576a7fa3fa18c246ca52f671ad3/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      )
    )
  )
}
