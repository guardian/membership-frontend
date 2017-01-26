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

  def get(bundleVariant: BundleVariant) = CachedAction { implicit request =>

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("d003e046432a83d2d42ed17d6f713dc986094e8d/0_0_960_800", Seq(960, 500), "png")
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
      case BundleVariant(A , _, _ ) => Ok(views.html.bundle.bundleSetA(
        heroOrientated,
        TouchpointBackend.Normal.catalog.supporter,
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters)
        ),
        bottomImageOrientated,
        bundleVariant))

      case BundleVariant(B , _, _ ) => Ok(views.html.bundle.bundleSetB(
        heroOrientated,
        TouchpointBackend.Normal.catalog.supporter,
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters)
        ),
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
      availableImages=ResponsiveImageGenerator("d003e046432a83d2d42ed17d6f713dc986094e8d/0_0_960_800", Seq(960, 500), "png")
    )
    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    Ok(views.html.bundle.thankYou(
        PageInfo(
          title = CopyConfig.copyTitleSupporters,
          url = request.path,
          description = Some(CopyConfig.copyDescriptionSupporters)
        ),
        bundleVariant, selectedOption, heroOrientated))
    }
}

object Bundle extends Bundle
