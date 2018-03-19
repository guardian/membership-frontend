package controllers

import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import views.support.Asset

class CacheBustedAssets(assets: Assets, override protected val controllerComponents: ControllerComponents) extends BaseController {
  def at(path: String): Action[AnyContent] =  {
    assets.at("/public", Asset.map(path), true)
  }
}
