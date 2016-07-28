package model

import com.gu.contentapi.client.model.v1.{Content, Tag}
import com.gu.memsub.images.{ResponsiveImageGroup => RIG}

case class CapiContent(headline: String, trailText: Option[String], mainPicture: Option[RIG], primaryTag: Tag)

object CapiContent {
  def apply(content: Content): CapiContent = {
    CapiContent(content.webTitle, content.fields.flatMap(_.trailText),
      ResponsiveImageGroup.fromContent(content), content.tags.head)
  }
}

