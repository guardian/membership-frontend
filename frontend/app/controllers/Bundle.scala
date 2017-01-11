package controllers

import com.gu.memsub.images.{Grid, ResponsiveImage, ResponsiveImageGenerator, ResponsiveImageGroup}
import configuration.CopyConfig
import model.{ContentItemOffer, FlashMessage, Nav, OrientatedImages}
import play.api.mvc.Controller
import services.{AuthenticationService, EmailService, GuardianContentService, TouchpointBackend}
import views.support.{Asset, PageInfo}

import scala.concurrent.Future

trait Bundle extends Controller {

  def get = CachedAction { implicit request =>

    val heroImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("Montage of The Guardian Headlines"),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("7b6e7b64f194b1f85bfc0791a23b8a25b72f39ba/0_0_1300_632", Seq(1300, 500), "png")
    )

    val heroOrientated = OrientatedImages(portrait = heroImage, landscape = heroImage)

    val detailImage = ResponsiveImageGroup(
      name=Some("intro"),
      metadata=Some(Grid.Metadata(
        description = Some("A scene in The Guardian editorial office."),
        byline = None,
        credit = None
      )),
      availableImages=ResponsiveImageGenerator("dcd0f0f703b1e784a3280438806f2feedf27dfab/0_0_1080_648", Seq(1080, 500))
    )

    val detailImageOrientated = OrientatedImages(portrait = detailImage, landscape = detailImage)

    Ok(views.html.bundle.elevatedSupporter(
      heroOrientated,
      TouchpointBackend.Normal.catalog.supporter,
      PageInfo(
        title = CopyConfig.copyTitleSupporters,
        url = request.path,
        description = Some(CopyConfig.copyDescriptionSupporters)
      ),
      detailImageOrientated))
  }
}

object Bundle extends Bundle
