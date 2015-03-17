package actions

import com.gu.membership.salesforce.Tier
import play.api.mvc.Results._
import play.api.mvc.{Call, RequestHeader}
import play.twirl.api.Html


object Fallbacks {

  def changeTier(implicit req: RequestHeader) = redirectTo(controllers.routes.TierController.change())

  def tierChangeEnterDetails(tier: Tier)(implicit req: RequestHeader) =
    redirectTo(controllers.routes.TierController.upgrade(tier))

  def notYetAMemberOn(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.tierChooser()).addingToSession("preJoinReturnUrl" -> request.uri)

  def chooseSigninOrRegister(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Login.chooseSigninOrRegister(request.uri))

  def joinStaffMembership(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.staff())

  def unauthorisedStaff(errorTemplate: Html)(implicit request: RequestHeader) =
    redirectTo(controllers.routes.StaffAuth.unauthorised()).flashing(
      "errorTemplate" -> errorTemplate.toString
    )

  def redirectTo(call: Call)(implicit req: RequestHeader) = SeeOther(call.absoluteURL(secure = true))
}
