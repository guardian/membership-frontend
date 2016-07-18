package model

import com.gu.contentapi.client.model.v1.{Content, Tag}
import com.gu.memsub.images.ResponsiveImageGroup

case class CapiContent(headline: String, trailText: Option[String], mainPicture: Option[ResponsiveImageGroup], primaryTag: Tag)

object CapiContent {
  def apply(content: Content): CapiContent = {
    CapiContent(content.webTitle, content.fields.flatMap(_.trailText),
      model.ResponsiveImageGroup.fromContent(content), content.tags.head)
  }
}

