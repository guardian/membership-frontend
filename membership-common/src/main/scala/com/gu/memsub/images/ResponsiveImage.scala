package com.gu.memsub.images
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.dsl._

object ResponsiveImageGenerator {
  def apply(id: String, sizes: Seq[Int], extension: String = "jpg"): Seq[ResponsiveImage] = {
    sizes.map { size =>
      ResponsiveImage(id.split("/").fold("https://media.guim.co.uk")({case (a, b) => a / b}) / s"$size.$extension", size)
    }
  }
}

case class ResponsiveImage(path: Uri, width: Int)

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
  val smallestImage: Uri = sortedImages.headOption.map(_.path).getOrElse("about:blank")
  val defaultImage: Uri = sortedImages.find(_.width > 300).map(_.path).getOrElse(smallestImage)

  val srcset = sortedImages.map { img =>
    img.path.toString + " " + img.width.toString + "w"
  }.mkString(", ")

  val metadataAltText = metadata.fold(altText)(_.description)

}
