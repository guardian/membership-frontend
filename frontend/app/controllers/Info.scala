package controllers

import com.gu.i18n.CountryGroup._
import configuration.CopyConfig
import forms.MemberForm._
import model._
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService, GuardianContentService, TouchpointBackend}
import views.support.{Asset, PageInfo}

import scala.concurrent.Future

trait Info extends Controller {

  def supporter = CachedAction { implicit request =>
    implicit val countryGroup = UK

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        metadata=Some(Grid.Metadata(
          description = Some("""|People from all walks of life gather in Westminster
            | on 17 June, in London, England, to demand the parliament backs
            | action on climate change""".stripMargin),
          byline = None,
          credit = Some("John Phillips/Getty Images")
        )),
        availableImages=Seq(ResponsiveImage(
          Asset.at("images/temp/supporter-intro.jpg"),
          1000
        ))
      ),
      ResponsiveImageGroup(
        name=Some("fearless"),
        metadata=Some(Grid.Metadata(
          description = Some("Reviewing designs on wall - Supporter"),
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
        name=Some("intro"),
        metadata=Some(Grid.Metadata(
          description = Some("People take part in a demonstration in Times Square, New York, November 27, 2014."),
          byline = None,
          credit = Some("Carlo Allegri/REUTERS")
        )),
        availableImages=ResponsiveImageGenerator(
          id="8eea3b3bd80eb2f8826b1cef75799d27a11e56e5/293_409_3206_1924",
          sizes=List(1000,500)
        )
      ),
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
      ),
      ResponsiveImageGroup(
        name=Some("join"),
        metadata=Some(Grid.Metadata(
          description = Some("Katharine Viner, editor-in-chief of the Guardian"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="e2e62954254813fd6781952a56f18bf20343ed0a/0_0_2000_1200",
          sizes=List(1000, 500)
        )
      )
    )


    Ok(views.html.info.supporterUSA(
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)),
      pageImages))
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
        item.content.fields.map(_("membershipAccess")).isEmpty && ! item.content.webTitle.startsWith("EXPIRED") && item.imgOpt.nonEmpty)

    Ok(views.html.info.offersAndCompetitions(TouchpointBackend.Normal.catalog, results))
  }

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def feedback = NoCacheAction { implicit request =>
    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    Ok(views.html.info.feedback(flashMsgOpt))
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
