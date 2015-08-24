package controllers

import configuration.CopyConfig
import model.{EventBrandCollection, PageInfo, ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import play.api.mvc.Controller
import services.{EventbriteService, GuardianLiveEventService, LocalEventService, MasterclassEventService}

trait FrontPage extends Controller {

  val liveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService


  def index =  CachedAction { implicit request =>

    val eventCollections = EventBrandCollection(
      liveEvents.getSortedByCreationDate.take(3),
      localEvents.getSortedByCreationDate.take(3),
      masterclassEvents.getSortedByCreationDate.take(3)
    )

    val pageImages = Seq(
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
      ),
      ResponsiveImageGroup(
        name=Some("space"),
        altText=Some("A home for big ideas"),
        availableImages=ResponsiveImageGenerator(
          id="ed9347da5fc1e55721b243a958d42fca1983d012/0_0_1140_684",
          sizes=List(500)
        )
      )
    )

    Ok(views.html.index(
      PageInfo(
        title=CopyConfig.copyTitleDefault,
        url="/",
        description=Some(CopyConfig.copyDescriptionDefault)
      ),
      pageImages,
      eventCollections
    ))
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

object FrontPage extends FrontPage {
  val liveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
