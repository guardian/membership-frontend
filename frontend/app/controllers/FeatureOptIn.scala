package controllers


import actions.CommonActions
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.Controller
import utils.{Feature, OnOrOff}

class FeatureOptIn(commonActions: CommonActions) extends Controller with LazyLogging {

  import commonActions.NoCacheAction

  def state(feature: Feature) = NoCacheAction { request =>
    val s = feature.stateFor(request)
    Ok(s"$feature: $s")
  }

  def setFeatureOnOrOff(feature: Feature, onOrOff: OnOrOff) = NoCacheAction {
    Ok(s"$feature $onOrOff : cookie dropped").withCookies(feature.cookieFor(onOrOff))
  }


}
