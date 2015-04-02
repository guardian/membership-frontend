package controllers

import configuration.CopyConfig
import forms.MemberForm._
import model.{ResponsiveImageGenerator, ResponsiveImageGroup, FlashMessage, PageInfo}
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService}
import scala.concurrent.Future

trait Info extends Controller {

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def about = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleAbout,
      request.path,
      Some(CopyConfig.copyDescriptionAbout)
    )
    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("About Membership"),
        availableImages=ResponsiveImageGenerator(
          id="0f30a12f5d00d2b0e445e81c171cd0168d3cf5a7/0_0_1140_684",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("guardian-live"),
        altText=Some("Guardian Live: Experience the Guardian brought to life"),
        availableImages=ResponsiveImageGenerator(
          id="76ef58a05920591099012edb80e7415379392a4c/0_0_1140_684",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("patrons"),
        altText=Some("Become a Patron and support the Guardian's future"),
        availableImages=ResponsiveImageGenerator(
          id="175fba5c61d2b398973befa5d6b49c1257740d5c/0_0_1140_683",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("masterclasses"),
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator(
          id="ae3ad30b485e9651a772e85dd82bae610f57a034/0_0_1140_684",
          sizes=List(1000, 500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("midland-goods-shed"),
        altText=Some("A home for big ideas"),
        availableImages=ResponsiveImageGenerator(
          id="ed9347da5fc1e55721b243a958d42fca1983d012/0_0_1140_684",
          sizes=List(1000, 500)
        )
      )
    )
    Ok(views.html.info.about(pageInfo, pageImages))
  }

  def feedback = NoCacheAction { implicit request =>
    val flashMsgOpt = request.flash.get("msg").map(FlashMessage.success)
    Ok(views.html.info.feedback(flashMsgOpt))
  }

  def giftingPlaceholder = NoCacheAction { implicit request =>
    Ok(views.html.info.giftingPlaceholder())
  }

  def supporter = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleSupporters,
      request.path,
      Some(CopyConfig.copyDescriptionSupporters)
    )
    val pageImages = Seq(
      ResponsiveImageGroup(
        name=Some("intro"),
        altText=Some("Introducing Supporter Membership"),
        availableImages=ResponsiveImageGenerator(
          id="a0e123f3289d26e0cf30153e64b89f4655a7e0d2/0_0_1999_1200",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("fearless"),
        altText=Some("Fearless, progressive and free from interference"),
        availableImages=ResponsiveImageGenerator(
          id="8cc033c84791f1583fc52f337d3c6c3ffb368f8e/0_0_1999_1200",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("polly-toynbee"),
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=ResponsiveImageGenerator(
          id="17a31ff294d3c77274091c5e078713fc06ef5cd2/0_0_1999_1200",
          sizes=List(1000, 500)
        )
      )
    )
    Ok(views.html.info.supporter(pageInfo, pageImages))
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
        altText=Some("Ensuring our independence"),
        availableImages=ResponsiveImageGenerator(
          id="e6459f638392c8176e277733f6f0802953100fa4/0_0_1140_683",
          sizes=List(1000,500)
        )
      ),
      ResponsiveImageGroup(
        name=Some("get-involved"),
        altText=Some("Choose to get involved"),
        availableImages=ResponsiveImageGenerator(
          id="d8f51bea15bf046df4d166cfd60771b9c24a631f/0_0_1140_683",
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
      )
    )
    Ok(views.html.info.patron(pageInfo, pageImages))
  }

  def subscriberOffer = GoogleAuthenticatedStaffAction { implicit request =>

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

  def submitFeedback = NoCacheAction.async { implicit request =>

    val userOpt = AuthenticationService.authenticatedUserFor(request)
    val uaOpt = request.headers.get(USER_AGENT)

    def sendFeedback(formData: FeedbackForm) = {
      EmailService.sendFeedback(formData, userOpt, uaOpt)

      Future.successful(Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us"))
    }

    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }


}

object Info extends Info
