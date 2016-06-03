package model

import com.gu.contentapi.client.model.v1.{Content, Element}
import com.gu.memsub.images.{ResponsiveImage, ResponsiveImageGroup => RIG}
import model.RichEvent.GridImage

case class OrientatedImages(portrait: RIG, landscape: RIG)

object ResponsiveImageGroup {

  /* The "main" image element of articles can have a variety of different
   * aspect-ratios, whereas the "thumbnail" should always have a consistent
   * aspect-ratio (5:3), meaning our images line up nicely in grid views etc.
   * Even though they're used as 'thumbnails', high resolution (2000*1200px)
   * asset-versions are generally available.
   * https://github.com/guardian/membership-frontend/pull/628
   */
  def hasConsistentAspectRatio(e: Element) = e.relation == "thumbnail"

  def fromContent(content: Content): Option[RIG] = for {
    elements <- content.elements
    element <- elements.find(hasConsistentAspectRatio)
  } yield com.gu.memsub.images.ResponsiveImageGroup(
    altText = element.assets.headOption.flatMap(_.typeData.flatMap(_.altText)),
    metadata = None,
    availableImages = for {
      asset <- element.assets
      typeData <- asset.typeData
      file <- typeData.secureFile
      width <- typeData.width
    } yield ResponsiveImage(file, width.toInt)
  )

  def fromGridImage(image: GridImage): Option[RIG] =
    if (image.assets.nonEmpty)
      Some(
        com.gu.memsub.images.ResponsiveImageGroup(
        altText = image.metadata.description,
        metadata = Some(image.metadata),
        availableImages = image.assets.map { asset =>
          val path = asset.secureUrl.getOrElse(asset.file)
          val width = asset.dimensions.width
          ResponsiveImage(path, width)
        }))
    else None
}
