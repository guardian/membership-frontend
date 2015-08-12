package controllers

import configuration.CopyConfig
import model.Benefits.ComparisonItem
import model.{PageInfo, ResponsiveImageGenerator, ResponsiveImageGroup}
import play.api.mvc.Controller

trait FrontPage extends Controller {

  def index =  CachedAction { implicit request =>

    val pageInfo = PageInfo(
      title=CopyConfig.copyTitleDefault,
      url="/",
      description=Some(CopyConfig.copyDescriptionDefault),
      hasBackgroundImage = false
    )

    val comparisonItems = Seq(
      ComparisonItem("Priority booking to all Guardian Live and Local events", false, true),
      ComparisonItem("Save 20% on Guardian Live and Local tickets", false, true),
      ComparisonItem("Bring a guest with the same discount and priority booking privileges", false, true),
      ComparisonItem("Save 20% on Guardian Masterclasses", false, true),
      ComparisonItem("Support fearless, open, independent journalism", true, true),
      ComparisonItem("Regular updates from the membership team", true, true),
      ComparisonItem("Exclusive offers and competitions", true, true),
      ComparisonItem("Membership card and annual gift", true, true),
      ComparisonItem("Highlights and live streams of selected Guardian Live events", true, true)
    )

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("experience"),
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator(
          id="eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("support"),
        altText=Some("Support the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("exclusive"),
        altText=Some("Exclusive content"),
        availableImages=ResponsiveImageGenerator(
          id="4bea41f93f7798ada3d572fe07b1e38dacb2a56e/0_0_2000_1200",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("brand-live"),
        altText=Some("Guardian Live"),
        availableImages=ResponsiveImageGenerator(
          id="ed27aaf7623aebc5c8c6d6c8340f247ef7b78ab0/0_0_2000_1200",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("brand-local"),
        altText=Some("Guardian Local"),
        availableImages=ResponsiveImageGenerator(
          id="889926d3c2ececf4ffd699f43713264697823251/0_0_2000_1200",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("brand-masterclasses"),
        altText=Some("Guardian Masterclasses"),
        availableImages=ResponsiveImageGenerator(
          id="ae3ad30b485e9651a772e85dd82bae610f57a034/0_0_1140_684",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("space"),
        altText=Some("A home for big ideas"),
        availableImages=ResponsiveImageGenerator(
          id="ed9347da5fc1e55721b243a958d42fca1983d012/0_0_1140_684",
          sizes=List(500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("patrons"),
        altText=Some("Patrons of The Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="a0b637e4dc13627ead9644f8ec9bd2cc8771f17d/0_0_2000_1200",
          sizes=List(500)
        )
      )
    )

    Ok(views.html.index(pageInfo, pageImages, comparisonItems))
  }

  def welcome = CachedAction { implicit request =>
    val slideShowImages = Seq(
      ResponsiveImageGroup(
        altText=Some("RIP Rock and Roll? (Guardian Live event): Emmy the Great"),
        availableImages=ResponsiveImageGenerator(
          id="3d2be6485a6b8f5948ba39519ceb0f76007ae8d8/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("A Life in Music - George Clinton (Guardian Live event)"),
        availableImages=ResponsiveImageGenerator(
          id="234dff81b39968199f501f4108189efab263a668/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Guardian Live with Russell Brand"),
        availableImages=ResponsiveImageGenerator(
          id="ecd5ccb67c093394c51f3db6779b044e3056f50c/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("A Life in Politics - Ken Clarke (Guardian Live event)"),
        availableImages=ResponsiveImageGenerator(
          id="192469f1bbd69247b066a202defb23ee166ede4d/0_0_2279_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator(
          id="eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("A Life in Music - George Clinton (Guardian Live event)"),
        availableImages=ResponsiveImageGenerator(
          id="eccf14ef0f9f4b672b3a7cc594676aa498827f4a/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        altText=Some("Behind the Headlines - What's all the fuss about feminism? (Guardian Live event): Bonnie Greer"),
        availableImages=ResponsiveImageGenerator(
          id="99c490b1a0863b3d30718e9985693a3ddcc4dc75/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.welcome(slideShowImages))
  }
}

object FrontPage extends FrontPage
