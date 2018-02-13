package controllers

import play.api.mvc.{AnyContent, Action, Controller}
import views.support.Asset

class CacheBustedAssets(assets: Assets) extends Controller {
  def at(path: String): Action[AnyContent] =  {
    assets.at("/public", Asset.map(path), true)
  }
}
