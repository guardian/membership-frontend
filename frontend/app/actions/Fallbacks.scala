package actions

import play.api.mvc.Results._
import play.api.mvc.{Call, RequestHeader}


object Fallbacks {

  def changeTier(implicit req: RequestHeader) = redirectTo(controllers.routes.TierController.change())

  def notYetAMemberOn(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joining.tierChooser()).addingToSession("preJoinReturnUrl" -> request.uri)

  def chooseSigninOrRegister(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Login.chooseSigninOrRegister(request.uri, None))

  def joinStaffMembership(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.staff())

  def unauthorisedStaff(error: String)(implicit request: RequestHeader) =
    redirectTo(controllers.routes.StaffAuth.unauthorised()).flashing("error" -> error)

  def redirectTo(call: Call)(implicit req: RequestHeader) = SeeOther(call.absoluteURL(secure = true))
}
