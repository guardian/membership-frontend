package actions

import com.gu.salesforce.PaidTier
import configuration.Config
import controllers.Assets.MOVED_PERMANENTLY
import play.api.mvc.Results._
import play.api.mvc.{Call, RequestHeader}
import play.twirl.api.Html

object Fallbacks {

  def supportRedirect(implicit request: RequestHeader) =
    Redirect("https://support.theguardian.com/", request.queryString, MOVED_PERMANENTLY)

  def maintenance(implicit request: RequestHeader) =
    TemporaryRedirect(controllers.routes.Outages.maintenanceMessage.absoluteURL(secure=true))

  def memberHome(implicit request: RequestHeader) =
    redirectTo(controllers.routes.FrontPage.welcome)

  def notYetAMemberOn(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.tierChooser()).addingToSession("preJoinReturnUrl" -> request.uri)

  def chooseRegister(implicit request: RequestHeader) = SeeOther(Config.idWebAppRegisterUrl(request.uri))

  def joinStaffMembership(implicit request: RequestHeader) =
    redirectTo(controllers.routes.Joiner.staff())

  def unauthorisedStaff(errorTemplate: Html)(implicit request: RequestHeader) =
    redirectTo(controllers.routes.StaffAuth.unauthorised()).flashing(
      "errorTemplate" -> errorTemplate.toString
    )

  def redirectTo(call: Call)(implicit req: RequestHeader) = SeeOther(call.absoluteURL(secure = true))
}
