package controllers

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.memsub.images.{Grid, ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import configuration.CopyConfig
import controllers.Redirects.redirectToSupporterPage
import model.{ContentItemOffer, FlashMessage, Nav, OrientatedImages}
import forms.MemberForm._
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService, GuardianContentService, TouchpointBackend}
import views.support.{Asset, PageInfo}
import com.netaporter.uri.dsl._
import com.netaporter.uri.Uri

import scala.concurrent.Future
import utils.RequestCountry._

trait Info extends Controller {
  def supporterRedirect = NoCacheAction { implicit request =>
    val countryGroup = request.getFastlyCountry.getOrElse(CountryGroup.RestOfTheWorld)

    val baseUrl: Uri = redirectToSupporterPage(countryGroup).absoluteURL.withScheme("https")
    val paramsToPropagate = Seq("INTCMP", "CMP")
    val urlWithParams = paramsToPropagate.foldLeft[Uri](baseUrl) { (url, param) =>
      // No need to filter out the params that aren't present because if the `?` method gets a key-value tuple
      // with value of None, that parameter will not be rendered when toString is called
      url ? (param -> request.getQueryString(param))
    }

    Redirect(urlWithParams, SEE_OTHER)
  }

  def supporterUK = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("""|People from all walks of life gather in Westminster
                             | on 17 June, in London, England, to demand the parliament backs
                             | action on climate change""".stripMargin),
        byline = None,
        credit = Some("John Phillips/Getty Images")
      )),
      availableImages=ResponsiveImageGenerator("17d84a219397dc81ec8d456ff0e97bf326d74015/0_127_4094_2457", Seq(2000, 1000))
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("fearless"),
        metadata=Some(Grid.Metadata(
          description = Some("Editors in the Guardian newsroom, London"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="8cc033c84791f1583fc52f337d3c6c3ffb368f8e/0_0_1999_1200",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("join"),
        metadata=Some(Grid.Metadata(
          description = Some("Polly Toynbee, Guardian columnist"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="17a31ff294d3c77274091c5e078713fc06ef5cd2/0_0_1999_1200",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.info.supporter(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      pageImages))
  }

  def supporterUSA = CachedAction { implicit request =>
    implicit val countryGroup = US

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("fearless"),
        metadata=Some(Grid.Metadata(
          description = Some("The Counted: people killed by police in the United States in 2015"),
          byline = Some("The Guardian US"),
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="201ae0837f996f47b75395046bdbc30aea587443/0_0_1140_684",
          sizes=List(1000,500)
        )
      )
    )

    val heroImages = OrientatedImages(
      portrait = ResponsiveImageGroup(availableImages =
        ResponsiveImageGenerator("8eea3b3bd80eb2f8826b1cef75799d27a11e56e5/1066_0_1866_2333", Seq(1866, 1600, 800))),
      landscape = ResponsiveImageGroup(availableImages =
        ResponsiveImageGenerator("8eea3b3bd80eb2f8826b1cef75799d27a11e56e5/0_613_3500_1500", Seq(3500, 2000, 1000, 500)))
    )

    Ok(
      views.html.info.supporterUSA(
        heroImages,
        TouchpointBackend.Normal.catalog.supporter,
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters),
          navigation = Nav.internationalLandingPageNavigation
        ),
        pageImages
      )
    )
  }

  def supporterEurope = CachedAction { implicit request =>
    implicit val countryGroup = Europe

    val hero = OrientatedImages(
      portrait = ResponsiveImageGroup(availableImages = Seq(
        ResponsiveImage(Asset.at("images/join-challenger/s_EU_hero01_980x980.jpg"), 980))),
      landscape = ResponsiveImageGroup(availableImages = Seq(
        ResponsiveImage(Asset.at("images/join-challenger/s_EU_hero01_1280x800.jpg"), 1280)))
    )

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("fearless"),
        availableImages=ResponsiveImageGenerator(
          id="88f98c8706beafeae6dc32886ccd71da60e6e7d7/0_0_5212_3129",
          sizes=List(1000,500)
        )
      )
    )

    Ok(
      views.html.info.supporterEurope(
        hero,
        TouchpointBackend.Normal.catalog.supporter,
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters),
          navigation = Nav.internationalLandingPageNavigation
        ),
        pageImages
      )
    )
  }

  def supporterFor(implicit countryGroup: CountryGroup) = CachedAction { implicit request =>

    val hero = OrientatedImages(
      portrait = ResponsiveImageGroup(availableImages =
        ResponsiveImageGenerator("6e2613b6442f1af7109e349eec38cffc0c54df6d/1872_0_3742_3744", Seq(2000, 1000, 500))),
      landscape = ResponsiveImageGroup(availableImages =
        ResponsiveImageGenerator("6e2613b6442f1af7109e349eec38cffc0c54df6d/0_0_5611_1979", Seq(2000, 1000, 500)))
    )

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("fearless"),
        availableImages=ResponsiveImageGenerator(
          id="88f98c8706beafeae6dc32886ccd71da60e6e7d7/0_0_5212_3129",
          sizes=List(1000,500)
        )
      )
    )

    Ok(
      views.html.info.supporterInternational(
        hero,
        TouchpointBackend.Normal.catalog.supporter,
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters),
          navigation = Nav.internationalLandingPageNavigation
        ),
        pageImages
      )
    )
  }

  def patron() = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val pageInfo = PageInfo(
      title = CopyConfig.copyTitlePatrons,
      url = request.path,
      description = Some(CopyConfig.copyDescriptionPatrons)
    )
    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("Patrons of the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("independence"),
        altText=Some("Katharine Viner, editor-in-chief of the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="a4856412e2bef82e6d1d4ce5220fe2391e3f5ca5/0_0_2000_1200",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("backstage-pass"),
        altText=Some("Backstage pass to the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="83afa3867ef76d82c86291f4387b5799c26e07f8/0_0_1140_684",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("get-involved"),
        altText=Some("Choose to get involved"),
        availableImages=ResponsiveImageGenerator(
          id="ed27aaf7623aebc5c8c6d6c8340f247ef7b78ab0/0_0_2000_1200",
          sizes=List(1000,500)
        )
      )
    )

    Ok(views.html.info.patron(
      patronPlans = TouchpointBackend.Normal.catalog.patron,
      partnerPlans = TouchpointBackend.Normal.catalog.partner,
      supporterPlans = TouchpointBackend.Normal.catalog.supporter,
      pageInfo = pageInfo,
      countryGroup = UK,
      pageImages = pageImages)
    )
  }

  def offersAndCompetitions = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val results =
      GuardianContentService.offersAndCompetitionsContent.map(ContentItemOffer).filter(item =>
        item.content.fields.flatMap(_.membershipAccess).isEmpty && ! item.content.webTitle.startsWith("EXPIRED") && item.imgOpt.nonEmpty)

    Ok(views.html.info.offersAndCompetitions(TouchpointBackend.Normal.catalog, results))
  }

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def feedback = NoCacheAction { implicit request =>
    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    Ok(views.html.info.feedback(flashMsgOpt, request.getQueryString("page")))
  }

  def submitFeedback = NoCacheAction.async { implicit request =>

    val userOpt = AuthenticationService.authenticatedUserFor(request).map(_.user)
    val uaOpt = request.headers.get(USER_AGENT)

    def sendFeedback(formData: FeedbackForm) = {
      EmailService.sendFeedback(formData, userOpt, uaOpt)

      Future.successful(Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us"))
    }

    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

}

object Info extends Info
