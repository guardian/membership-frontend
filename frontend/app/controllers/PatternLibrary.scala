package controllers

import model.{ResponsiveImageGenerator, ResponsiveImageGroup}
import play.api.mvc.Controller
import services.{TouchpointBackend, GuardianLiveEventService, EventbriteService}
import play.api.libs.concurrent.Execution.Implicits._

trait PatternLibrary extends Controller {
  val guLiveEvents: EventbriteService

  val pageImages = Seq(
    ResponsiveImageGroup(
      name=Some("sample-1"),
      altText=Some("Patrons of the Guardian"),
      availableImages=ResponsiveImageGenerator(
        id="8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
        sizes=List(1000,500)
      )
    ),
    ResponsiveImageGroup(
      name=Some("sample-2"),
      altText=Some("Ensuring our independence"),
      availableImages=ResponsiveImageGenerator(
        id="e6459f638392c8176e277733f6f0802953100fa4/0_0_1140_683",
        sizes=List(1000,500)
      )
    )
  )

  def patterns = NoCacheAction.async { implicit request =>
    TouchpointBackend.Normal.tierPricing.map(pricing =>
      Ok(views.html.patterns.patterns(
        pricing,
        guLiveEvents.events,
        pageImages)))
  }

}

object PatternLibrary extends PatternLibrary {
  val guLiveEvents = GuardianLiveEventService
}
