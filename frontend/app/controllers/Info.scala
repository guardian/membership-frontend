package controllers

import actions.ActionRefiners.PlannedOutageProtection
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.memsub.images.{Grid, ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import configuration.CopyConfig
import controllers.Redirects.redirectToSupporterPage
import forms.FeedbackForm
import model.{ContentItemOffer, FlashMessage, Nav, OrientatedImages}
import play.api.mvc.{Controller, RequestHeader}
import services._
import tracking.RedirectWithCampaignCodes._
import utils.RequestCountry._
import views.support.{Asset, PageInfo}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

trait Info extends Controller with LazyLogging {
  def supporterRedirect(countryGroup: Option[CountryGroup]) = NoCacheAction { implicit request =>
    val determinedCountryGroup = (countryGroup orElse request.getFastlyCountryCode).getOrElse(CountryGroup.RestOfTheWorld)
    redirectWithCampaignCodes(redirectToSupporterPage(determinedCountryGroup).url, SEE_OTHER)
  }

  val CachedAndOutageProtected = CachedAction andThen PlannedOutageProtection

   def supporterUSA = CachedAndOutageProtected { implicit request =>
    implicit val countryGroup = US

    val pageImages = Seq(
      ResponsiveImageGroup(
        name = Some("fearless"),
        metadata = Some(Grid.Metadata(
          description = Some("The Counted: people killed by police in the United States in 2015"),
          byline = Some("The Guardian US"),
          credit = None
        )),
        availableImages = ResponsiveImageGenerator(
          id = "201ae0837f996f47b75395046bdbc30aea587443/0_0_1140_684",
          sizes = List(1000, 500)
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

  def supporterAustralia = NoCacheAction { implicit request =>
    logger.info(s"supporter-au-impression ${abtests.SupporterLandingPage.describeParticipation}")

    if (abtests.SupporterLandingPage.allocate(request).exists(_.showNewDesign)) {
      supporterAustraliaNew(request)
    } else {
      supporterAustraliaOld(request)
    }
  }

  def supporterAustraliaOld(request: RequestHeader)(implicit token: play.filters.csrf.CSRF.Token) = {
    implicit val countryGroup = Australia

    val heroImage = ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("""Same-Sex marriage activists march in the street during a Same-Sex Marriage rally in Sydney, Sunday, Aug. 9, 2015""".stripMargin),
        byline = None,
        credit = Some("Carol Cho/AAP")
      )),
      availableImages = ResponsiveImageGenerator("73f50662f5834f4194a448e966637fc88c0b36f6/0_0_5760_3840", Seq(2000, 1000))
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val pageImages = Seq(
      ResponsiveImageGroup(
        name = Some("coral"),
        metadata = Some(Grid.Metadata(
          description = Some("The impact of coral bleaching at Lizard Island on the Great Barrier Reef: (left) the coral turns white, known as 'bleaching', in March 2016; (right) the dead coral is blanketed by seaweed in May 2016"),
          byline = None,
          credit = None
        )),
        availableImages = ResponsiveImageGenerator(
          id = "03d7db325026227b0832bfcd17b2f16f8eb5cfed/0_167_5000_3000",
          sizes = List(1000, 500)
        )
      ))

    Ok(views.html.info.supporterAustraliaOld(
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

  def supporterAustraliaNew(request: RequestHeader)(implicit token: play.filters.csrf.CSRF.Token) = {
    implicit val countryGroup = Australia

    val heroImage = ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Montage of The Guardian Australia Headlines"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("8a2003a809b699701111f10d3a0bef3c8e2ffa03/0_0_1000_486", Seq(1000, 500), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Your Guardian Membership certificate"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("71b8bebab82bdead12273ff4e299a04dccad0d20/0_0_1080_610", Seq(1080, 500), "png")
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.info.supporterAustraliaNew(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters),
        navigation = Nil
      ),
      detailImageOrientated))
  }

  def supporterFor(implicit countryGroup: CountryGroup) = CachedAndOutageProtected { implicit request =>

    val heroImage = ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("7b6e7b64f194b1f85bfc0791a23b8a25b72f39ba/0_0_1300_632", Seq(1300, 500), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name = Some("intro"),
      metadata = Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages = ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 500))
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.info.supporter(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      detailImageOrientated))
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
        name = Some("intro"),
        altText = Some("Patrons of the Guardian"),
        availableImages = ResponsiveImageGenerator(
          id = "8caacf301dd036a2bbb1b458cf68b637d3c55e48/0_0_1140_683",
          sizes = List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name = Some("independence"),
        altText = Some("Katharine Viner, editor-in-chief of the Guardian"),
        availableImages = ResponsiveImageGenerator(
          id = "a4856412e2bef82e6d1d4ce5220fe2391e3f5ca5/0_0_2000_1200",
          sizes = List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name = Some("backstage-pass"),
        altText = Some("Backstage pass to the Guardian"),
        availableImages = ResponsiveImageGenerator(
          id = "83afa3867ef76d82c86291f4387b5799c26e07f8/0_0_1140_684",
          sizes = List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name = Some("get-involved"),
        altText = Some("Choose to get involved"),
        availableImages = ResponsiveImageGenerator(
          id = "ed27aaf7623aebc5c8c6d6c8340f247ef7b78ab0/0_0_2000_1200",
          sizes = List(1000, 500)
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
        item.content.fields.flatMap(_.membershipAccess).isEmpty && !item.content.webTitle.startsWith("EXPIRED") && item.imgOpt.nonEmpty)

    Ok(views.html.info.offersAndCompetitions(TouchpointBackend.Normal.catalog, results))
  }

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  val identityService = IdentityService(IdentityApi)



  def feedback = NoCacheAction.async { implicit request =>
    val authenticatedUser = AuthenticationService.authenticatedUserFor(request)
    val name = authenticatedUser.flatMap(_.displayName)

    val identityUser = authenticatedUser.map { user => identityService.getFullUserDetails(user)(IdentityRequest(request)) }
    val email  = identityUser.map(_.map(u => Some(u.primaryEmailAddress))).getOrElse(Future.successful(None))


    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    email.map { email =>
      Ok(views.html.info.feedback(flashMsgOpt, request.getQueryString("page"), name, email))
    }
  }

  def submitFeedback = NoCacheAction.async { implicit request =>

    val userOpt = AuthenticationService.authenticatedUserFor(request).map(_.user)
    val uaOpt = request.headers.get(USER_AGENT)

    val identityUser = userOpt.map { user => identityService.getFullUserDetails(user)(IdentityRequest(request)) }
    val email  = identityUser.map(_.map(u => Some(u.primaryEmailAddress))).getOrElse(Future.successful(None))


    def sendFeedback(formData: FeedbackForm) = {
      email.map{email=>
        EmailService.sendFeedback(formData, userOpt, email, uaOpt)
        Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us")
      }

    }

    FeedbackForm.form.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

}

object Info extends Info
