package controllers

import play.api.mvc.{AnyContent, Action, Controller}
import views.support.Asset

class CacheBustedAssets(asset: Assets) extends Controller {
  def at(path: String): Action[AnyContent] =  {
    asset.at("/public", Asset.map(path), true)
  }
}
