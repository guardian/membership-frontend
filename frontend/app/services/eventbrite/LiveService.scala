package services.eventbrite

import configuration.Config
import model.Eventbrite.EBEvent
import model.RichEvent.GridImage
import services.{GuardianContentService, GridService}

import scala.concurrent.Future


abstract class LiveService extends EventbriteService {
   val gridService = GridService(Config.gridConfig.url)
   val contentApiService = GuardianContentService

   def gridImageFor(event: EBEvent) =
     event.mainImageUrl.fold[Future[Option[GridImage]]](Future.successful(None))(gridService.getRequestedCrop)
 }
