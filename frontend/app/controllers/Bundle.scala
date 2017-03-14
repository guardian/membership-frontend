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
}

object Bundle extends Bundle
