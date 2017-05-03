package controllers

import abtests.BundleVariant
import com.gu.memsub.images.{Grid, ResponsiveImageGenerator, ResponsiveImageGroup}
import configuration.CopyConfig
import model._
import play.api.mvc.Controller
import services._
import views.support.PageInfo

trait Bundle extends Controller {

  private val landingMainImage = ResponsiveImageGroup(
    availableImages = ResponsiveImageGenerator(
      id = "d1a7088f8f2a367b0321528f081777c9b5618412/0_0_3578_2013",
      sizes = Seq(2000, 1000, 500)
    )
  )

  private val landingMainImageWide = ResponsiveImageGroup(
    availableImages = ResponsiveImageGenerator(
      id = "bce7d14f7f837a4f6c854d95efc4b1eab93a8c65/0_0_5200_720",
      sizes = Seq(2000, 1000)
    )
  )

  private val landingPatronsImage = ResponsiveImageGroup(
    availableImages = ResponsiveImageGenerator(
      id = "137d6b217a27acddf85512657d04f6490b9e0bb1/1638_0_3571_2009",
      sizes = Seq(1000, 500, 140)
    )
  )

  private val landingEventsImage = ResponsiveImageGroup(
    availableImages = ResponsiveImageGenerator(
      id = "5f18c6428e9f31394b14215fe3c395b8f7b4238a/500_386_2373_1335",
      sizes = Seq(1000, 500, 140)
    )
  )

  def get(bundleVariant: BundleVariant) = CachedAction { implicit request =>

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("62a6d58f49c10d5864d024c16ba05554a32cee5a/0_0_480_275", Seq(480), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val bottomImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("2a88bdcbe6ff74ca49f203d70b4b8f1fab885d71/0_76_460_550", Seq(460), "png")
    )

    val bottomImageOrientated = OrientatedImages(portrait = bottomImage, landscape = bottomImage)

    bundleVariant match {
      case _:BundleVariant => Ok(views.html.bundle.bundleSetA(
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
      availableImages=ResponsiveImageGenerator("62a6d58f49c10d5864d024c16ba05554a32cee5a/0_0_480_275", Seq(480), "png")
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
