package model

import com.gu.contentapi.client.model.Content
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
  def apply(content: Content): Option[ResponsiveImageGroup] = for {
    elements <- content.elements
    element <- elements.find(_.relation == "thumbnail")
  } yield ResponsiveImageGroup(
    altText = element.assets.headOption.flatMap(_.typeData.get("altText")),
    metadata = None,
    availableImages = for {
      asset <- element.assets
      file <- asset.file
      width <- asset.typeData.get("width")
    } yield ResponsiveImage(file.replace("http://static", "https://static-secure"), width.toInt)
  )

  def apply(image: GridImage): ResponsiveImageGroup = ResponsiveImageGroup(
    altText = image.metadata.description,
    metadata = Some(image.metadata),
    availableImages = image.assets.map { asset =>
      val path = asset.secureUrl.getOrElse(asset.file)
      val width = asset.dimensions.width
      ResponsiveImage(path, width)
    }
  )
}

case class ResponsiveImage(path: String, width: Int)

case class ResponsiveImageGroup(
  name: Option[String] = None,
  altText: Option[String],
  metadata: Option[Grid.Metadata] = None,
  availableImages: Seq[ResponsiveImage]
) {

  private val sortedImages = availableImages.sortBy(_.width)

  val smallestImage = sortedImages.head.path
  val defaultImage = sortedImages.find(_.width > 300).map(_.path).getOrElse(smallestImage)

  val srcset = sortedImages.map { img =>
    img.path + " " + img.width.toString + "w"
  }.mkString(", ")

}
