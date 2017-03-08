package controllers

import abtests.{BundleVariant}
import abtests.Distribution._
import com.gu.memsub.images.{Grid, ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import configuration.CopyConfig
import model.{ContentItemOffer, FlashMessage, Nav, OrientatedImages}
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService, GuardianContentService, TouchpointBackend}
import views.support.{Asset, PageInfo}

trait Bundle extends Controller {

  private def landingMainImage () = {

    val imgId = "d1a7088f8f2a367b0321528f081777c9b5618412/0_0_3578_2013"
    val imgSizes = Seq(2000, 1000, 500)

    ResponsiveImageGroup(
      availableImages = ResponsiveImageGenerator(imgId, imgSizes)
    )

  }

  private def landingMainImageWide () = {

    val imgId = "bce7d14f7f837a4f6c854d95efc4b1eab93a8c65/0_0_5200_720"
    val imgSizes = Seq(2000, 1000)

    ResponsiveImageGroup(
      availableImages = ResponsiveImageGenerator(imgId, imgSizes)
    )

  }

  private def landingPatronsImage () = {

    val imgId = "137d6b217a27acddf85512657d04f6490b9e0bb1/1638_0_3571_2009"
    val imgSizes = Seq(1000, 500, 140)

    ResponsiveImageGroup(
      availableImages = ResponsiveImageGenerator(imgId, imgSizes)
    )

  }

  private def landingEventsImage () = {

    val imgId = "5f18c6428e9f31394b14215fe3c395b8f7b4238a/500_386_2373_1335"
    val imgSizes = Seq(1000, 500, 140)

    ResponsiveImageGroup(
      availableImages = ResponsiveImageGenerator(imgId, imgSizes)
    )

  }

  def get(bundleVariant: BundleVariant) = CachedAction { implicit request =>

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("a1b0524bdd81bf9f5e92d199c0977c04b59731ec/0_0_480_400", Seq(480), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val bottomImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("6bf759b538128aed0f90cebe8b2465875e85c7ce/0_0_460_650", Seq(460), "png")
    )

    val bottomImageOrientated = OrientatedImages(portrait = bottomImage, landscape = bottomImage)

    bundleVariant match {
      case BundleVariant(_, _, _, _, _) => Ok(views.html.bundle.bundleSetA(
        heroOrientated,
        TouchpointBackend.Normal.catalog.supporter,
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters)
        ),
        request.getQueryString("returnUrl"),
        bottomImageOrientated,
        bundleVariant))
    }
  }

  def thankYou(bundleVariant: BundleVariant, selectedOption: String) = CachedAction { implicit request =>
    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("a1b0524bdd81bf9f5e92d199c0977c04b59731ec/0_0_480_400", Seq(480), "png")
    )
    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    Ok(views.html.bundle.thankYou(
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters)
        ),
        bundleVariant,
        selectedOption,
        heroOrientated,
        request.getQueryString("returnUrl")))
    }

  def landing() = CachedAction { implicit request =>

    val pageInfo = PageInfo(
      title = CopyConfig.copyTitleSupporters,
      url = request.path,
      description = Some(CopyConfig.copyDescriptionSupporters)
    )

    Ok(views.html.bundle.bundlesLanding(
      pageInfo,
      landingMainImage,
      landingMainImageWide,
      landingPatronsImage,
      landingEventsImage
    ))

  }

}

object Bundle extends Bundle
