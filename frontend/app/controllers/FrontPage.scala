package controllers

import model.RichEvent.EventBrandCollection
import model._
import play.api.mvc.Controller
import services._
import play.api.libs.concurrent.Execution.Implicits._

trait FrontPage extends Controller {

  val liveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService

  def index = CachedAction.async { implicit request =>

    val eventCollections = EventBrandCollection(
      liveEvents.getSortedByCreationDate.take(3),
      localEvents.getSortedByCreationDate.take(3),
      masterclassEvents.getSortedByCreationDate.take(3)
    )

    val midlandGoodsShedImages = Seq(
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Midland Goods Shed and East handyside Canopy at King's Cross"),
          byline = None,
          credit = Some("John Sturrock")
        )),
        availableImages=ResponsiveImageGenerator(
          id="ae8a3ef9e568fbc5df4ceab27bf6cd0847fe3f06/0_357_8688_5213",
          sizes=List(500, 140)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Construction work in the East handyside Canopy at King's Cross"),
          byline = None,
          credit = Some("John Sturrock")
        )),
        availableImages=ResponsiveImageGenerator(
          id="51963d023d9fa7885cad228d663104e4d04dc8b2/0_334_4998_2999",
          sizes=List(500, 140)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Midland Goods Shed, King's Cross. Guardian Forum Event."),
          byline = None,
          credit = Some("Bennetts Associates")
        )),
        availableImages=ResponsiveImageGenerator(
          id="6adbea0e05e56945a77894aca7eb9c363789567e/27_438_4933_2961",
          sizes=List(500, 140)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("The renovation of the Midland Goods Shed Office and East Handyside Canopy, King's Cross"),
          byline = None,
          credit = Some("John Sturrock")
        )),
        availableImages=ResponsiveImageGenerator(
          id="81b36e7a40d74ff3c95c664c3b89d49914471e95/0_0_5000_2999",
          sizes=List(500, 140)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Midland Goods Shed, King's Cross"),
          byline = None,
          credit = Some("John Sturrock")
        )),
        availableImages=ResponsiveImageGenerator(
          id="84081e14d97e33ad65d026233cdb87d4c3723d6a/206_0_4793_2875",
          sizes=List(500, 140)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Midland Goods Shed, King's Cross"),
          byline = None,
          credit = Some("Bennetts Associates")
        )),
        availableImages=ResponsiveImageGenerator(
          id="d19c696109fd8d0be40bb8a89917555a4d7f852d/0_80_1754_1052",
          sizes=List(500, 140)
        )
      )
    )

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("patrons"),
        metadata=Some(Grid.Metadata(Some("Patrons of The Guardian"), None, None)),
        availableImages=ResponsiveImageGenerator(
          id="a0b637e4dc13627ead9644f8ec9bd2cc8771f17d/0_0_2000_1200",
          sizes=List(500)
        )
      )
    )

    TouchpointBackend.Normal.tierPricing.map { pricing =>
          Ok(views.html.index(pricing,
                              pageImages,
                              midlandGoodsShedImages,
                              eventCollections))
    }
  }

  def welcome = CachedAction { implicit request =>
    val slideShowImages = Seq(
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("RIP Rock and Roll? (Guardian Live event): Emmy the Great"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="3d2be6485a6b8f5948ba39519ceb0f76007ae8d8/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("A Life in Music - George Clinton (Guardian Live event)"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="234dff81b39968199f501f4108189efab263a668/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Guardian Live with Russell Brand"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="ecd5ccb67c093394c51f3db6779b044e3056f50c/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("A Life in Politics - Ken Clarke (Guardian Live event)"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="192469f1bbd69247b066a202defb23ee166ede4d/0_0_2279_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="eab86e9c81414932e0d50a1cd609dccfc20ca5d2/0_0_2279_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("A Life in Music - George Clinton (Guardian Live event)"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="eccf14ef0f9f4b672b3a7cc594676aa498827f4a/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        metadata=Some(Grid.Metadata(
          description = Some("Behind the Headlines - What's all the fuss about feminism? (Guardian Live event): Bonnie Greer"),
          byline = None, credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="99c490b1a0863b3d30718e9985693a3ddcc4dc75/0_0_2280_1368",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.welcome(PageInfo("Welcome", request.path, None), slideShowImages))
  }
}

object FrontPage extends FrontPage {
  val liveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
