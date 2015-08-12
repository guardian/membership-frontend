package controllers

import play.api.mvc.Controller
import scala.concurrent.Future
import configuration.CopyConfig
import forms.MemberForm._
import model._
import services.{AuthenticationService, EmailService}

trait Info extends Controller {

  def supporter = CachedAction { implicit request =>

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        metadata=Some(Grid.Metadata(
          description = Some("Katharine Viner and Alan Rusbridger"),
          byline = None,
          credit = None
        )),
        availableImages=ResponsiveImageGenerator(
          id="f55167b65375c2f078a88d09856bc46670d21f57/0_0_2000_1200",
          sizes=List(1000,500)
        )
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

    Ok(views.html.info.supporter(PageInfo(
      CopyConfig.copyTitleSupporters,
      request.path,
      Some(CopyConfig.copyDescriptionSupporters)
    ), pageImages))
  }

  def supporterUSA = CachedAction { implicit request =>

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

    Ok(views.html.info.supporterUSA(PageInfo(
      CopyConfig.copyTitleSupporters,
      request.path,
      Some(CopyConfig.copyDescriptionSupporters)
    ), pageImages))

  }

  def patron() = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitlePatrons,
      request.path,
      Some(CopyConfig.copyDescriptionPatrons)
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
    Ok(views.html.info.patron(pageInfo, pageImages))
  }

  def subscriberOffer = CachedAction { implicit request =>

    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("Guardian Live Audience"),
        availableImages=ResponsiveImageGenerator(
          id="38dafd8e470b0d7b3399034f0ccbcce63a0dff25/0_0_1140_684",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("guardian-live"),
        altText=Some("Guardian Live"),
        availableImages=ResponsiveImageGenerator(
          id="9fa0dfc49eb89ec70efe163755564bf0f632fabf/0_0_2279_1368",
          sizes=List(1000, 500)
        )
      )
    )

    Ok(views.html.info.subscriberOffer(pageImages))
  }

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def feedback = NoCacheAction { implicit request =>
    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    Ok(views.html.info.feedback(flashMsgOpt))
  }

  def submitFeedback = NoCacheAction.async { implicit request =>

    val userOpt = AuthenticationService.authenticatedUserFor(request)
    val uaOpt = request.headers.get(USER_AGENT)

    def sendFeedback(formData: FeedbackForm) = {
      EmailService.sendFeedback(formData, userOpt, uaOpt)

      Future.successful(Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us"))
    }

    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

  def giftingPlaceholder = NoCacheAction { implicit request =>
    Ok(views.html.info.giftingPlaceholder())
  }

}

object Info extends Info
