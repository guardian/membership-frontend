package controllers

import com.gu.i18n._
import com.gu.stripe.Stripe
import configuration.Config
import com.gu.stripe.Stripe.Serializer._
import forms.MemberForm.supportForm
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, JsString, Json}
import play.api.mvc._
import services.{AuthenticationService, TouchpointBackend}
import com.netaporter.uri.dsl._
import views.support.{TestTrait, _}

import scalaz.syntax.std.option._
import scala.concurrent.Future
import utils.RequestCountry._
import com.netaporter.uri.dsl._
import com.netaporter.uri.{PathPart, Uri}
import Redirects.getRedirectCountryCodeGiraffe

object Giraffe extends Controller {

  val social: Set[Social] = Set(
    Twitter("I've just contributed to the Guardian. Join me in supporting independent journalism https://membership.theguardian.com/contribute"),
    Facebook("https://membership.theguardian.com/contribute")
  )


  val chargeId = "charge_id"
  val maxAmount: Option[Int] = 2000.some

  def contributeRedirect = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.RestOfTheWorld)
    val url = MakeGiraffeRedirectURL(request, countryGroup)
    Redirect(url, SEE_OTHER)
  }

  // Once things have settled down and we have a reasonable idea of what might
  // and might not vary between different countries, we should merge these country-specific
  // controllers & templates into a single one which varies on a number of parameters
  def contribute(countryGroup: CountryGroup) = OptionallyAuthenticatedAction { implicit request =>
    val stripe = request.touchpointBackend.giraffeStripeService
    val isUAT = (request.touchpointBackend == TouchpointBackend.TestUser)
    val cmp = request.getQueryString("CMP")
    val intCmp = request.getQueryString("INTCMP")
    val chosenVariants: ChosenVariants = Test.getContributePageVariants(countryGroup, request)
    val pageInfo = PageInfo(
      title = "Support the Guardian | Contribute today",
      url = request.path,
      image = Some("https://media.guim.co.uk/5719a2b724efd8944e0222d57c839a7d2b6e39b3/0_0_1440_864/1000.jpg"),
      stripePublicKey = Some(stripe.publicKey),
      description = Some("By making a contribution, you'll be supporting independent journalism that speaks truth to power"),
      navigation = Seq.empty,
      customSignInUrl = Some((Config.idWebAppUrl / "signin") ? ("skipConfirmation" -> "true"))
    )
    Ok(views.html.giraffe.contribute(pageInfo,maxAmount,countryGroup,isUAT, chosenVariants, cmp, intCmp, CreditCardExpiryYears(java.time.LocalDate.now().getYear, 10)))
      .withCookies(Test.createCookie(chosenVariants.v1), Test.createCookie(chosenVariants.v2))
  }

  def thanks(countryGroup: CountryGroup, redirectUrl: String) = NoCacheAction { implicit request =>
    request.session.get(chargeId).fold(
      Redirect(redirectUrl, SEE_OTHER)
    )( id =>
      Ok(views.html.giraffe.thankyou(PageInfo(
        title = "Thank you for supporting the Guardian",
        url = request.path,
        image = None,
        description = Some("Your contribution is much appreciated, and will help us to maintain our independent, investigative journalism."),
        navigation = Seq.empty
      ), id, social, countryGroup))
    )
  }


  def contributeUK = contribute(CountryGroup.UK)
  def contributeUSA = contribute(CountryGroup.US)
  def contributeAustralia = contribute(CountryGroup.Australia)
  def contributeEurope = contribute(CountryGroup.Europe)

  def thanksUK = thanks(CountryGroup.UK, routes.Giraffe.contributeUK().url)
  def thanksUSA = thanks(CountryGroup.US, routes.Giraffe.contributeUSA().url)
  def thanksAustralia = thanks(CountryGroup.Australia, routes.Giraffe.contributeAustralia().url)
  def thanksEurope = thanks(CountryGroup.Europe, routes.Giraffe.contributeEurope().url)



  def pay = OptionallyAuthenticatedAction.async { implicit request =>
    val stripe = request.touchpointBackend.giraffeStripeService
    val identity = request.touchpointBackend.identityService
    supportForm.bindFromRequest().fold[Future[Result]]({ withErrors =>
      Future.successful(BadRequest(JsArray(withErrors.errors.map(k => JsString(k.key)))))
    },{ f =>
      val metadata = Map(
        "marketing-opt-in" -> f.marketing.toString,
        "email" -> f.email,
        "name" -> f.name,
        "abTests" -> f.abTests.toString,
        "ophanId" -> f.ophanId,
        "cmp" -> f.cmp.mkString,
        "intcmp" -> f.intcmp.mkString
      ) ++ AuthenticationService.authenticatedUserFor(request).map("idUser" -> _.user.id) ++ f.postCode.map("postcode" -> _)
      val res = stripe.Charge.create(maxAmount.fold((f.amount*100).toInt)(max => Math.min(max * 100, (f.amount * 100).toInt)), f.currency, f.email, "Your contribution", f.token, metadata)

      AuthenticationService.authenticatedUserFor(request).map { user =>
        identity.updateUserMarketingPreferences(IdentityRequest(request), user, f.marketing)
      }

      val redirect = f.currency match {
        case USD => routes.Giraffe.thanksUSA().url
        case AUD => routes.Giraffe.thanksAustralia().url
        case EUR => routes.Giraffe.thanksEurope().url
        case _ => routes.Giraffe.thanksUK().url
      }

      res.map { charge =>
        Ok(Json.obj("redirect" -> redirect))
          .withSession(chargeId -> charge.id)
      }.recover {
        case e: Stripe.Error => BadRequest(Json.toJson(e))
      }
    })
  }
}

object MakeGiraffeRedirectURL {
  def apply(request: Request[AnyContent], countryGroup: CountryGroup) = {
    val x = Uri.parse(request.uri).withScheme("https")
    x.copy(pathParts = Seq(PathPart(getRedirectCountryCodeGiraffe(countryGroup).id)) ++ x.pathParts)
  }
}

object CreditCardExpiryYears {
  def apply(currentYear: Int, offset: Int): List[Int] = {
    val currentYearShortened = currentYear % 100
    val subsequentYears = (currentYearShortened to currentYearShortened + offset - 2) map { _ + 1}
    currentYearShortened :: subsequentYears.toList
  }
}

