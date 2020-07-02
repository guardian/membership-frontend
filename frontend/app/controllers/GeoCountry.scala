package controllers

import actions.CommonActions
import play.api.mvc.{BaseController, ControllerComponents}
import utils.RequestCountry._

class GeoCountry(commonActions: CommonActions,  override protected val controllerComponents: ControllerComponents) extends BaseController {
  import commonActions.NoCacheAction

  def getCountry () = NoCacheAction {
    request =>
      Ok(request.getFastlyCountry.map(_.alpha2).getOrElse("Missing Country"))

  }
}
