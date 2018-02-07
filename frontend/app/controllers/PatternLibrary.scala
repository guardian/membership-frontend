package controllers

import com.gu.i18n.CountryGroup._
import com.gu.memsub.images.{ResponsiveImageGenerator, ResponsiveImageGroup}
import play.api.mvc.Controller
import services.{EventbriteCollectiveServices, EventbriteService, GuardianLiveEventService, TouchpointBackend}

class PatternLibrary(eventbriteService: EventbriteCollectiveServices) extends Controller {
  val guLiveEvents = eventbriteService.guardianLiveEventService
  implicit val countryGroup = UK

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

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns(
      TouchpointBackend.Normal.catalog,
      guLiveEvents.events,
      pageImages))
  }

}
