package actions

import com.gu.memsub.subsv2.CatalogPlan.PaidMember
import com.gu.memsub.subsv2.MonthYearPlans
import com.gu.salesforce.PaidTier
import play.api.mvc.Results._
import play.api.mvc.{Call, Cookie, RequestHeader}
import play.twirl.api.Html
import configuration.Config
import views.html.joiner.form.paymentB
import views.support.{CountryWithCurrency, IdentityUser, PageInfo}

import scala.util.Random


sealed trait CheckoutFlowVariant {
  val testId: String
  val identitySkin: String
}

object CheckoutFlowVariant {
  val cookieName = "ab-checkout-flow"

  val all = Seq[CheckoutFlowVariant](A,B)

  def deriveFlowVariant(implicit request: RequestHeader): CheckoutFlowVariant =
    getFlowVariantFromRequestCookie(request).getOrElse(CheckoutFlowVariant.all(Random.nextInt(CheckoutFlowVariant.all.size)))

  def getFlowVariantFromRequestCookie(request: RequestHeader): Option[CheckoutFlowVariant] = for {
    cookieValue <- request.cookies.get(cookieName)
    variant <- CheckoutFlowVariant.lookup(cookieValue.value)
  } yield variant

  case object A extends CheckoutFlowVariant {
    override val testId: String = "test-A"
    override val identitySkin: String = "members"
  }

  case object B extends CheckoutFlowVariant {
    override val testId: String = "test-B"
    override val identitySkin: String = "membersB"
  }

  def lookup(name: String): Option[CheckoutFlowVariant] = all.find(_.testId == name)
}

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
