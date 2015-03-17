package model

case class ResponsiveImage(path: String, width: Int)

case class ResponsiveImageGroup(
  name: Option[String] = None,
  altText: String,
  availableImages: Seq[ResponsiveImage]
) {

  private val sortedImages = availableImages.sortBy(_.width)

  val smallestImage = sortedImages.head.path
  val defaultImage = sortedImages.find(_.width > 300).map(_.path).getOrElse(smallestImage)

  val srcset = sortedImages.map { img =>
    img.path + " " + img.width.toString() + "w"
  }.mkString(", ")

}
