package model

import com.gu.contentapi.client.model.v1.Content

abstract class RichContent(content: Content) {
  val imgOpt = ResponsiveImageGroup.fromContent(content)
}

case class ContentItem(content: Content) extends RichContent(content)

case class ContentItemOffer(content: Content) extends RichContent(content) {
  val tagTitleOpt = content.tags.map { tag =>
    /**
     * Only show tag title if in membership-offers or membership-competitions
     * 
     * Remove "Membership" prefix from membership-offers and membership-competitions tag title,
     * leaving just Offers or Competitions. Membership prefix is redundant here.
     */
    tag.id.toLowerCase match {
      case "membership/membership-offers" | "membership/membership-competitions" => Some(tag.webTitle.replace("Membership ", ""))
      case _ => None
    }
  }
}
