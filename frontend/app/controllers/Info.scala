package controllers

import configuration.CopyConfig
import forms.MemberForm._
import model.{ResponsiveImageGenerator, ResponsiveImageGroup, FlashMessage, PageInfo}
import play.api.mvc.Controller
import services.EmailService
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
        name=Some("masterclasses"),
        altText=Some("Guardian Live event: Pussy Riot - art, sex and disobedience"),
        availableImages=ResponsiveImageGenerator("ae3ad30b485e9651a772e85dd82bae610f57a034/0_0_1140_684", List(1000, 500))
      ),
      ResponsiveImageGroup(
        name=Some("midland-goods-shed"),
        altText=Some("A home for big ideas"),
        availableImages=ResponsiveImageGenerator("ed9347da5fc1e55721b243a958d42fca1983d012/0_0_1140_684", List(1000, 500))
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
        name=Some("polly-toynbee"),
        altText=Some("If you read the Guardian, join the Guardian"),
        availableImages=ResponsiveImageGenerator("17a31ff294d3c77274091c5e078713fc06ef5cd2/0_0_1999_1200", List(1000, 500))
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
        name=Some("backstage-pass"),
        altText=Some("Backstage pass to the Guardian"),
        availableImages=ResponsiveImageGenerator("83afa3867ef76d82c86291f4387b5799c26e07f8/0_0_1140_684", List(1000, 500))
      )
    )
    Ok(views.html.info.patron(pageInfo, pageImages))
  }

  def submitFeedback = NoCacheAction.async { implicit request =>
    feedbackForm.bindFromRequest.fold(_ => Future.successful(BadRequest), sendFeedback)
  }

  private def sendFeedback(formData: FeedbackForm) = {
    EmailService.sendFeedback(formData)

    Future.successful(Redirect(routes.Info.feedback()).flashing("msg" -> "Thank you for contacting us"))
  }
}

object Info extends Info
