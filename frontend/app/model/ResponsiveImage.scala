package model

case class ResponsiveImage(path: String, width: Int)

case class ResponsiveImageGroup(
  name: Option[String] = None,
  altText: String,
  availableImages: Seq[ResponsiveImage]
) {
  val defaultImage = availableImages.headOption.map(_.path)
  val srcset = availableImages.map { img =>
    img.path + " " + img.width.toString() + "w"
  }.mkString(", ")
}
