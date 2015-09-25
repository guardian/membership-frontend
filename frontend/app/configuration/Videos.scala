package configuration

import com.netaporter.uri.dsl._
import model.{Video, ResponsiveImageGroup, ResponsiveImageGenerator}

object Videos {

  private val whatIsMembershipPlaceholder = ResponsiveImageGenerator(
    // ff2fdc0b1cd540445ab0c57b3efe8f08eba2e213
    id="0f0b186b9acd3c32ae47e445e07fe5b128250d7f/0_0_1800_1080",
    sizes=List(1000, 500)
  )

  val whatIsMembership = Video(
    srcUrl="//www.youtube.com/embed/oRowh6Nzt4c?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("What is Guardian Members?"),
        availableImages=whatIsMembershipPlaceholder
      )
    )
  )

  private val supportersPlaceholder = ResponsiveImageGenerator(
    id="267569fecb462c61718f7e8cf50a8995ebddee5d/0_0_2280_1368",
    sizes=List(1000, 500)
  )

  val supporters = Video(
    srcUrl="//www.youtube.com/embed/pIg3BCr1mwY?enablejsapi=1&wmode=transparent",
    posterImage=Some(
      ResponsiveImageGroup(
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=supportersPlaceholder
      )
    )
  )

}
