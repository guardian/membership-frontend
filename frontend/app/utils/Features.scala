package utils

import enumeratum._
import play.api.mvc.{Cookie, RequestHeader}
import utils.OnOrOff.On

sealed trait Feature extends EnumEntry {
  val cookieName = s"feature-$entryName"

  def cookieFor(onOrOff: OnOrOff) = Cookie(cookieName, onOrOff.entryName, httpOnly = false)

  def turnedOnFor(request: RequestHeader): Boolean = stateFor(request) == On

  def stateFor(request: RequestHeader): OnOrOff = {
    request.cookies.get(cookieName).flatMap(cookie => OnOrOff.withNameOption(cookie.value)).getOrElse(OnOrOff.Off)
  }
}

object Feature extends PlayEnum[Feature] {

  val values = findValues

  case object MergedRegistration extends Feature

}

sealed trait OnOrOff extends EnumEntry {
  val opposite: OnOrOff
}

object OnOrOff extends PlayEnum[OnOrOff] {

  val values = findValues

  case object On extends OnOrOff {
    val opposite = Off
  }
  case object Off extends OnOrOff {
    val opposite = On
  }

}
