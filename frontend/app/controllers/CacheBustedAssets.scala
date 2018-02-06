package controllers

import play.api.mvc.{AnyContent, Action, Controller}
import views.support.Asset
import javax.inject.{Inject, Singleton}

@Singleton
class CacheBustedAssets @Inject()() extends Controller{
  def at(path: String): Action[AnyContent] =  {
    controllers.Assets.at("/public", Asset.map(path), true)
  }
}
