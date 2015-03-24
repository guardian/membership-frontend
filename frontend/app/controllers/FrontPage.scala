package controllers

import model.{ResponsiveImageGenerator, ResponsiveImage, ResponsiveImageGroup}
import play.api.mvc.Controller

trait FrontPage extends Controller {

  def index = CachedAction { implicit request =>

    val slideShowImages = Seq(
      ResponsiveImageGroup(
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator(
          id="eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368",
          sizes=List(2000, 1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Guardian Live with Russell Brand"),
        availableImages=ResponsiveImageGenerator(
          id="da867c363957d748bd04cfd9d4890033203a58c6/0_0_2279_1368",
          sizes=List(2000, 1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("A life in music: Jimmy Page"),
        availableImages=ResponsiveImageGenerator(
          id="733834b0b9f84e4367cf676919008bfd88007f25/0_0_2279_1368",
          sizes=List(2000, 1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Guardian Live with Russell Brand"),
        availableImages=ResponsiveImageGenerator(
          id="e736acb945406974844bea77ca49b2c1e450013e/0_0_2279_1368",
          sizes=List(2000, 1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Observer Ideas - Stories that inspire: Tinie Tempah"),
        availableImages=ResponsiveImageGenerator(
          id="1e687530a52ef14cd9c1eacb1e8757600c382f86/0_0_2279_1368",
          sizes=List(2000, 1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Guardian Live event: Vivienne Westwood"),
        availableImages=ResponsiveImageGenerator(
          id="85d46476d07fb744327c60a4c5117a7ddc2796d0/0_0_2279_1368",
          sizes=List(2000, 1000, 500)
        )
      )
    )

    Ok(views.html.index(slideShowImages))
  }
}

object FrontPage extends FrontPage
