package model

import com.gu.contentapi.client.model.Content

abstract class RichContent(content: Content) {
  val imgOpt = ResponsiveImageGroup(content)
}

case class ContentItem(content: Content) extends RichContent(content)

case class ContentItemOffer(content: Content) extends RichContent(content) {
  val tagTitleOpt = content.tags.find { tag =>
    tag.id.toLowerCase match {
      case "membership/membership-offers" | "membership/membership-competitions" => true
      case _ => false
    }
  }
}
