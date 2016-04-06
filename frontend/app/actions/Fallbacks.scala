package actions

import com.gu.salesforce.PaidTier
import play.api.mvc.Results._
import play.api.mvc.{Call, RequestHeader}
import play.twirl.api.Html

object Fallbacks {

  def changeTier(implicit req: RequestHeader) =
    redirectTo(controllers.routes.TierController.change())

  def memberHome(implicit request: RequestHeader) =
    redirectTo(controllers.routes.FrontPage.welcome)

  def tierChangeEnterDetails(tier: PaidTier)(implicit req: RequestHeader) =
    redirectTo(controllers.routes.TierController.upgrade(tier))

  def notYetAMemberOn(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.tierChooser())
      .addingToSession("preJoinReturnUrl" -> request.uri)

  def chooseRegister(implicit request: RequestHeader) =
    SeeOther(RegistrationUri.parse(request))

  def joinStaffMembership(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.staff())

  def unauthorisedStaff(errorTemplate: Html)(implicit request: RequestHeader) =
    redirectTo(controllers.routes.StaffAuth.unauthorised()).flashing(
        "errorTemplate" -> errorTemplate.toString
    )

  def redirectTo(call: Call)(implicit req: RequestHeader) =
    SeeOther(call.absoluteURL(secure = true))
}
