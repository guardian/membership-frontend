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
import tracking.RedirectWithCampaignCodes._

import scala.concurrent.Future
import utils.RequestCountry._

trait Info extends Controller {
  def supporterRedirect(countryGroup: Option[CountryGroup]) = NoCacheAction { implicit request =>
    val determinedCountryGroup = (countryGroup orElse request.getFastlyCountry).getOrElse(CountryGroup.RestOfTheWorld)
    redirectWithCampaignCodes(redirectToSupporterPage(determinedCountryGroup).url, SEE_OTHER)
  }

  def supporterUK = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("1cc958628a26fa42347858c801975abf73de21dc/0_0_1280_632", Seq(1280, 1000), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 648))
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.info.elevatedSupporter(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      detailImageOrientated))
  }

  def supporterAustralia = CachedAction { implicit request =>
    implicit val countryGroup = Australia

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("""Same-Sex marriage activists march in the street during a Same-Sex Marriage rally in Sydney, Sunday, Aug. 9, 2015""".stripMargin),
        byline = None,
        credit = Some("Carol Cho/AAP")
      )),
      availableImages=ResponsiveImageGenerator("73f50662f5834f4194a448e966637fc88c0b36f6/0_0_5760_3840", Seq(2000, 1000))
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("coral"),
        metadata=Some(Grid.Metadata(
          description = Some("The impact of coral bleaching at Lizard Island on the Great Barrier Reef: (left) the coral turns white, known as 'bleaching', in March 2016; (right) the dead coral is blanketed by seaweed in May 2016"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="03d7db325026227b0832bfcd17b2f16f8eb5cfed/0_167_5000_3000",
          sizes=List(1000,500)
        )
      ))

    Ok(views.html.info.supporterAustralia(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters),
        navigation = Nil
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
