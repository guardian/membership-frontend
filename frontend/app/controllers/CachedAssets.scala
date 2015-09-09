package controllers

import model.SVG
import play.Logger
import play.api.mvc.{Action, Controller}
import views.support.Asset

import scala.concurrent.ExecutionContext.Implicits.global

object CachedAssets extends Controller {

  def dynamicLogo = Action { request =>
    val randomLogo = SVG.Logos.getRandomLogo
    Asset.inlineResource(randomLogo.path).fold(Cached(1)(NotFound)) { image =>
      Cached(60)(Ok(image).as("image/svg+xml"))
    }
  }

  def at(path: String, file: String, aggressiveCaching: Boolean = false) = Action.async { request =>
    controllers.Assets.at(path, file, aggressiveCaching).apply(request).recover {
      case e: RuntimeException => {
        Logger.warn(s"Asset run time exception for path $path $file. Does this file exist?", e)
        Cached(NotFound)
      }
    }.map { result =>
      if (result.header.headers.contains(CACHE_CONTROL)) result else Cached(2)(result)
    }
  }
}
