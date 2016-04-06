package controllers

import play.Logger
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CachedAssets extends Controller {

  def at(path: String, file: String, aggressiveCaching: Boolean = false) =
    Action.async { request =>
      controllers.Assets
        .at(path, file, aggressiveCaching)
        .apply(request)
        .recover {
          case e: RuntimeException => {
              Logger.warn(
                  s"Asset run time exception for path $path $file. Does this file exist?",
                  e)
              Cached(NotFound)
            }
        }
        .map { result =>
          if (result.header.headers.contains(CACHE_CONTROL)) result
          else Cached(2)(result)
        }
    }

  def bookmarkletAt(path: String, file: String) = Action.async { request =>
    controllers.Assets
      .at(path, file)
      .apply(request)
      .recover {
        case e: RuntimeException => {
            Logger.warn(
                s"Bookmarklet run time exception for path $path $file. Does this file exist?",
                e)
            Cached(NotFound)
          }
      }
      .map { result =>
        // Always cache bookmarklets for an hour
        Cached(3600)(result)
      }
  }
}
