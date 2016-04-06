package model

import com.gu.contentapi.client.model.{Element, Content}
import views.support.Asset
import model.RichEvent.GridImage

object ResponsiveImageGenerator {
  def apply(id: String, sizes: Seq[Int]): Seq[ResponsiveImage] = {
    sizes.map { size =>
      ResponsiveImage(s"https://media.guim.co.uk/$id/$size.jpg", size)
    }
  }
}

object ResponsiveImageGroup {

  /* The "main" image element of articles can have a variety of different
   * aspect-ratios, whereas the "thumbnail" should always have a consistent
   * aspect-ratio (5:3), meaning our images line up nicely in grid views etc.
   * Even though they're used as 'thumbnails', high resolution (2000*1200px)
   * asset-versions are generally available.
   * https://github.com/guardian/membership-frontend/pull/628
   */
  def hasConsistentAspectRatio(e: Element) = e.relation == "thumbnail"

  def fromContent(content: Content): Option[ResponsiveImageGroup] =
    for {
      elements <- content.elements
      element <- elements.find(hasConsistentAspectRatio)
    } yield
      ResponsiveImageGroup(
          altText = element.assets.headOption
              .flatMap(_.typeData.get("altText")),
          metadata = None,
          availableImages = for {
              asset <- element.assets
              file <- asset.typeData.get("secureFile")
              width <- asset.typeData.get("width")
            } yield ResponsiveImage(file, width.toInt)
      )

  def fromGridImage(image: GridImage): Option[ResponsiveImageGroup] =
    if (image.assets.nonEmpty)
      Some(
          ResponsiveImageGroup(altText = image.metadata.description,
                               metadata = Some(image.metadata),
                               availableImages = image.assets.map { asset =>
                               val path = asset.secureUrl.getOrElse(asset.file)
                               val width = asset.dimensions.width
                               ResponsiveImage(path, width)
                             }))
    else None
}

case class ResponsiveImage(path: String, width: Int)

case class ResponsiveImageGroup(
    name: Option[String] = None,
    altText: Option[String] = None,
    metadata: Option[Grid.Metadata] = None,
    availableImages: Seq[ResponsiveImage]
    ) {

  private val sortedImages = availableImages.sortBy(_.width)

  // We expect to have at least one availableImage. In the rare case we don't,
  // set the path to about:blank, which is still a valid URL whose resource is an empty string.
  // http://www.w3.org/TR/2011/WD-html5-20110525/fetching-resources.html#about:blank
  val smallestImage =
    sortedImages.headOption.map(_.path).getOrElse("about:blank")
  val defaultImage =
    sortedImages.find(_.width > 300).map(_.path).getOrElse(smallestImage)

  val srcset = sortedImages.map { img =>
    img.path + " " + img.width.toString + "w"
  }.mkString(", ")

  val metadataAltText = metadata.fold(altText)(_.description)
}
