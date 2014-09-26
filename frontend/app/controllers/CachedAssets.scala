package controllers
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc.{Action, Controller}

object CachedAssets extends Controller {

  def at(path: String, file: String, aggressiveCaching: Boolean = false) = Action.async { request =>
    controllers.Assets.at(path, file, aggressiveCaching).apply(request).map { result =>
      if (result.header.headers.contains(CACHE_CONTROL)) result else Cached(result)
    }

  }
}
