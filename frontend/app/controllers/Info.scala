package controllers

import actions.ActionRefiners.PlannedOutageProtection
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.memsub.images.{Grid, ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.netaporter.uri.dsl._
import configuration.CopyConfig
import controllers.Redirects.redirectToSupporterPage
import forms.MemberForm._
import model.{ContentItemOffer, FlashMessage, Nav, OrientatedImages}
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService, GuardianContentService, TouchpointBackend}
import tracking.RedirectWithCampaignCodes._
import utils.RequestCountry._
import views.support.{Asset, PageInfo}

import scala.concurrent.Future

trait Info extends Controller {
  def supporterRedirect(countryGroup: Option[CountryGroup]) = NoCacheAction { implicit request =>
    val determinedCountryGroup = (countryGroup orElse request.getFastlyCountryCode).getOrElse(CountryGroup.RestOfTheWorld)
    redirectWithCampaignCodes(redirectToSupporterPage(determinedCountryGroup).url, SEE_OTHER)
  }

  val CachedAndOutageProtected = CachedAction andThen PlannedOutageProtection

  def supporterUK = CachedAndOutageProtected { implicit request =>
    implicit val countryGroup = UK

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("7b6e7b64f194b1f85bfc0791a23b8a25b72f39ba/0_0_1300_632", Seq(1300, 500), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 500))
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.info.elevatedSupporterUK(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      detailImageOrientated))
  }

  def supporterAustralia = CachedAndOutageProtected { implicit request =>
    implicit val countryGroup = Australia

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("7b6e7b64f194b1f85bfc0791a23b8a25b72f39ba/0_0_1300_632", Seq(1300, 500), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 500))
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.info.elevatedSupporterAU(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      detailImageOrientated))
  }


  def supporterUSA = CachedAndOutageProtected { implicit request =>
    implicit val countryGroup = US

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("7b6e7b64f194b1f85bfc0791a23b8a25b72f39ba/0_0_1300_632", Seq(1300, 500), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 500))
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.info.elevatedSupporterUS(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      detailImageOrientated)
    )
  }

  def supporterEurope = CachedAndOutageProtected { implicit request =>
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

  def supporterFor(implicit countryGroup: CountryGroup) = CachedAndOutageProtected { implicit request =>

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

  def patron() = CachedAndOutageProtected { implicit request =>
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
