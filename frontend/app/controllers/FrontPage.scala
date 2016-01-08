package controllers

import com.gu.i18n.CountryGroup._
import model.RichEvent.EventBrandCollection
import model._
import play.api.mvc.Controller
import services._
import views.support.PageInfo

trait FrontPage extends Controller {
  val liveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService

  def index = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val eventCollections = EventBrandCollection(
      liveEvents.getSortedByCreationDate.take(3),
      localEvents.getSortedByCreationDate.take(3),
      masterclassEvents.getSortedByCreationDate.take(3)
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

    Ok(views.html.index(
      TouchpointBackend.Normal.catalog,
      pageImages,
      eventCollections))
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

    Ok(views.html.welcome(PageInfo(title = "Welcome", url = request.path), slideShowImages))
  }
}

object FrontPage extends FrontPage {
  val liveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
