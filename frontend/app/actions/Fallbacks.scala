package actions

import abtests.CheckoutFlowVariant
import com.gu.salesforce.PaidTier
import configuration.Config
import play.api.mvc.Results._
import play.api.mvc.{Call, Cookie, RequestHeader}
import play.twirl.api.Html

object Fallbacks {

  def changeTier(implicit req: RequestHeader) = redirectTo(controllers.routes.TierController.change())

  def maintenance(implicit request: RequestHeader) =
    TemporaryRedirect(controllers.routes.Outages.maintenanceMessage.absoluteURL(secure=true))

  def memberHome(implicit request: RequestHeader) =
    redirectTo(controllers.routes.FrontPage.welcome)

  def tierChangeEnterDetails(tier: PaidTier)(implicit req: RequestHeader) =
    redirectTo(controllers.routes.TierController.upgrade(tier, None))

  def notYetAMemberOn(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.tierChooser()).addingToSession("preJoinReturnUrl" -> request.uri)

  def chooseRegister(implicit request: RequestHeader) = {
    val flowSelected = CheckoutFlowVariant.deriveFlowVariant(request)

    SeeOther(Config.idWebAppRegisterUrl(request.uri, flowSelected)).withCookies(Cookie(CheckoutFlowVariant.cookieName, flowSelected.testId))
  }

  def joinStaffMembership(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.staff())

  def unauthorisedStaff(errorTemplate: Html)(implicit request: RequestHeader) =
    redirectTo(controllers.routes.StaffAuth.unauthorised()).flashing(
      "errorTemplate" -> errorTemplate.toString
    )

  def redirectTo(call: Call)(implicit req: RequestHeader) = SeeOther(call.absoluteURL(secure = true))
}
